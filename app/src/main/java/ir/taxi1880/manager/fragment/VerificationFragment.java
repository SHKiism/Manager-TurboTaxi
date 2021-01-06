package ir.taxi1880.manager.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import ir.taxi1880.manager.R;
import ir.taxi1880.manager.app.EndPoints;
import ir.taxi1880.manager.app.MyApplication;
import ir.taxi1880.manager.helper.FragmentHelper;
import ir.taxi1880.manager.helper.KeyBoardHelper;
import ir.taxi1880.manager.helper.PhoneNumberValidation;
import ir.taxi1880.manager.helper.TypefaceUtil;
import ir.taxi1880.manager.okHttp.RequestHelper;


public class VerificationFragment extends Fragment {
    Unbinder unbinder;
    String mobileNumber;

    @BindView(R.id.edtMobileNumber)
    EditText edtMobileNumber;

    @BindView(R.id.vfEnter)
    ViewFlipper vfEnter;

    @OnClick(R.id.btnEnter)
    void onPressEnter() {
        mobileNumber = edtMobileNumber.getText().toString();

        if (mobileNumber.isEmpty()) {
            MyApplication.Toast("شماره موبایل را وارد کنید", Toast.LENGTH_SHORT);
            return;
        }

        if (!PhoneNumberValidation.isValid(mobileNumber)) {
            MyApplication.Toast("شماره موبایل نا معتبر میباشد", Toast.LENGTH_SHORT);
            return;
        }

        mobileNumber = mobileNumber.startsWith("0") ? mobileNumber : "0" + mobileNumber;

        KeyBoardHelper.hideKeyboard();
        verification(mobileNumber);
    }

    @OnClick(R.id.txtAnotherWayToLogin)
    void onPressAnotherWayToLogin() {
        FragmentHelper
                .toFragment(MyApplication.currentActivity, new LoginFragment())
                .setAddToBackStack(false)
                .replace();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_verification, container, false);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        unbinder = ButterKnife.bind(this, view);
        TypefaceUtil.overrideFonts(view);

        return view;
    }

    private void verification(String phoneNumber) {
        if (vfEnter != null) {
            vfEnter.setDisplayedChild(1);
        }

        RequestHelper.builder(EndPoints.VERIFICATION)
                .addParam("phoneNumber", phoneNumber)
                .listener(onVerificationCallBack)
                .post();

    }

    private RequestHelper.Callback onVerificationCallBack = new RequestHelper.Callback() {
        @Override
        public void onResponse(Runnable reCall, Object... args) {
            MyApplication.handler.post(() -> {
                try {
//                    {"success":true,"message":"با موفقیت ارسال شد","data":{"repetitionTime":120}}
                    JSONObject object = new JSONObject(args[0].toString());
                    boolean success = object.getBoolean("success");
                    String message = object.getString("message");
                    if (success) {
                        JSONObject objData = object.getJSONObject("data");
                        int repetitionTime = objData.getInt("repetitionTime");
                        MyApplication.prefManager.setRepetitionTime(repetitionTime);
                        Bundle bundle = new Bundle();
                        bundle.putString("mobileNumber", mobileNumber);
                        FragmentHelper.toFragment(MyApplication.currentActivity, new CheckVerificationFragment()).setArguments(bundle).setAddToBackStack(false).replace();
                    } else {
//                        {"success":false,"message":"محدودیت زمانی","data":{}}
                    }
                    MyApplication.Toast(message, Toast.LENGTH_SHORT);

                    if (vfEnter != null) {
                        vfEnter.setDisplayedChild(0);
                    }
                } catch (Exception e) {
                    if (vfEnter != null) {
                        vfEnter.setDisplayedChild(0);
                    }
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void onFailure(Runnable reCall, Exception e) {
            MyApplication.handler.post(() -> {
                if (vfEnter != null) {
                    vfEnter.setDisplayedChild(0);
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}