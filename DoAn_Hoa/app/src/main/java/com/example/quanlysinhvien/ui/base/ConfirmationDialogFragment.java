package com.example.quanlysinhvien.ui.base;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.quanlysinhvien.R;
import java.util.function.Consumer;

public class ConfirmationDialogFragment extends BaseDialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_ICON_RES = "icon_res";
    private static final String ARG_POSITIVE_TEXT = "positive_text";
    private static final String ARG_NEGATIVE_TEXT = "negative_text";

    private Consumer<Boolean> resultConsumer;

    public static ConfirmationDialogFragment newInstance(
            String title, String message, @DrawableRes int iconRes, 
            String positiveButtonText, String negativeButtonText
    ) {
        ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putInt(ARG_ICON_RES, iconRes);
        args.putString(ARG_POSITIVE_TEXT, positiveButtonText);
        args.putString(ARG_NEGATIVE_TEXT, negativeButtonText);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setOnResultListener(Consumer<Boolean> consumer) {
        this.resultConsumer = consumer;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_confirmation;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            setDialogTitle(args.getString(ARG_TITLE));
            setDialogMessage(args.getString(ARG_MESSAGE));
            setDialogIcon(args.getInt(ARG_ICON_RES, R.drawable.ic_baseline_check_circle_24));
            setPositiveButtonText(args.getString(ARG_POSITIVE_TEXT, "Confirm"));
            setNegativeButtonText(args.getString(ARG_NEGATIVE_TEXT, "Cancel"));
        }

        return dialog;
    }

    @Override
    protected void onPositiveClicked() {
        if (resultConsumer != null) {
            resultConsumer.accept(true);
        }
        dismiss();
    }
    
    @Override
    protected void onNegativeClicked() {
        if (resultConsumer != null) {
            resultConsumer.accept(false);
        }
        dismiss();
    }
}
