package org.cyducks.satark.registration.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import org.cyducks.satark.R;
import org.cyducks.satark.databinding.FragmentRoleSelectBinding;
import org.cyducks.satark.registration.viewmodel.RegistrationViewModel;
import org.cyducks.satark.util.UserRole;


public class RoleSelectFragment extends Fragment {
    FragmentRoleSelectBinding viewBinding;
    RegistrationViewModel viewModel;




    public RoleSelectFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        viewBinding = FragmentRoleSelectBinding.inflate(getLayoutInflater());
//        viewBindings = FragmentRoleSelectBinding.inflate(getLayoutInflater());


        // Inflate the layout for this fragment

        viewModel = new ViewModelProvider(requireActivity()).get(RegistrationViewModel.class);

        viewBinding.ownerCard.setOnClickListener(v -> {
            viewBinding.driverCard.setChecked(false);
            viewBinding.ownerCard.setChecked(true);
        });

        viewBinding.driverCard.setOnClickListener(v -> {
            viewBinding.ownerCard.setChecked(false);
            viewBinding.driverCard.setChecked(true);
        });

        viewBinding.nextBtn.setOnClickListener(v ->  {

            if(viewBinding.ownerCard.isChecked()) {
                viewModel.setUserRole(UserRole.MODERATOR);
                Navigation.findNavController(v).navigate(R.id.action_roleSelectFragment_to_documentInputFragment);
            }
            else if(viewBinding.driverCard.isChecked()) {
                viewModel.setUserRole(UserRole.CIVILIAN);
                Navigation.findNavController(v).navigate(R.id.action_roleSelectFragment_to_registrationSuccessFragment);
            }

        });

        return viewBinding.getRoot();
    }
}