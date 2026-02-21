package com.example.quanlysinhvien.ui.base;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.quanlysinhvien.R;
import com.google.android.material.button.MaterialButton;

public abstract class BaseDialogFragment extends DialogFragment {

    private ImageView ivDialogIcon;
    private TextView tvDialogTitle, tvDialogMessage;
    private MaterialButton btnPositive, btnNegative;
    private LinearLayout customContent;

    protected abstract int getLayoutId();
    protected abstract void onPositiveClicked();
    protected void onNegativeClicked() { dismiss(); }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(getLayoutId(), null);
        
        ivDialogIcon = view.findViewById(R.id.iv_dialog_icon);
        tvDialogTitle = view.findViewById(R.id.tv_dialog_title);
        tvDialogMessage = view.findViewById(R.id.tv_dialog_message);
        btnPositive = view.findViewById(R.id.btn_positive);
        btnNegative = view.findViewById(R.id.btn_negative);
        customContent = view.findViewById(R.id.dialog_custom_content);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnPositive.setOnClickListener(v -> onPositiveClicked());
        btnNegative.setOnClickListener(v -> onNegativeClicked());
        
        // Make the background transparent to show the card's rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }

    protected void setDialogIcon(int iconResId) {
        if (ivDialogIcon != null) {
            ivDialogIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), iconResId));
        }
    }
    
    protected void setDialogTitle(String title) {
        if(tvDialogTitle != null) tvDialogTitle.setText(title);
    }
    
    protected void setDialogMessage(String message) {
        if(tvDialogMessage != null) tvDialogMessage.setText(message);
    }
    
    protected void setPositiveButtonText(String text) {
        if(btnPositive != null) btnPositive.setText(text);
    }
    
    protected void setNegativeButtonText(String text) {
        if(btnNegative != null) btnNegative.setText(text);
    }
    
    protected View getCustomContentView() {
        return customContent;
    }
}
