package nyc.c4q.jordansmith.finefree;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;
import nyc.c4q.jordansmith.finefree.model.Car;
import nyc.c4q.jordansmith.finefree.model.ParkingCameraResponse;
import nyc.c4q.jordansmith.finefree.network.parking_camera_violations.ParkingCameraViolationsClient;
import nyc.c4q.jordansmith.finefree.recyclerview.ViolationsAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by jordansmith on 2/18/17.
 */

public class FragmentHome extends Fragment {
    private static final int THREE_SECONDS = 3000;
    private RecyclerView violationRV;
    private ViolationsAdapter mViolationsAdapter = new ViolationsAdapter();
    private String licensePlate;
    private String carName;
    private TextView nameOfCar;
    private TextView plateOfCar;
    private TextView onlyShownIfNoDataTV;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.home_fragment_layout, container, false);
        violationRV = (RecyclerView) rootView.findViewById(R.id.violations_recyclerview);
        nameOfCar = (TextView) rootView.findViewById(R.id.name_textview);
        plateOfCar = (TextView) rootView.findViewById(R.id.license_textview);
        onlyShownIfNoDataTV = (TextView) rootView.findViewById(R.id.replace_rv_when_empty);

        Bundle bundle = getArguments();
        if(bundle != null){
            Car car = (Car) bundle.getSerializable(ActivityMain.PLATE_KEY);
            licensePlate = car.getLicensePlate();
            plateOfCar.setText("Plate #: " + licensePlate);
            carName = car.getName();
        }

        violationRV.setLayoutManager(new LinearLayoutManager(getContext()));

        AlphaInAnimationAdapter alphaAdapter = new AlphaInAnimationAdapter(mViolationsAdapter);
        alphaAdapter.setDuration(THREE_SECONDS);
        alphaAdapter.setInterpolator(new OvershootInterpolator());
        violationRV.setAdapter(new ScaleInAnimationAdapter(alphaAdapter));
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchViolations();
    }

    private void fetchViolations() {
        ParkingCameraViolationsClient
                .getInstance()
                .getResponseByPlate(licensePlate.toUpperCase())
                .enqueue(new Callback<List<ParkingCameraResponse>>() {
                    @Override
                    public void onResponse(Call<List<ParkingCameraResponse>> call, Response<List<ParkingCameraResponse>> response) {
                        List<ParkingCameraResponse> violationsList = parseResponseForOutstandingViolations(response.body());
                        if(violationsList.isEmpty()){
                            violationRV.setVisibility(View.GONE);
                            nameOfCar.setVisibility(View.GONE);
                            onlyShownIfNoDataTV.setVisibility(View.VISIBLE);
                        }
                        String violationSize = (String.valueOf(violationsList.size()));
                        nameOfCar.setText(violationSize + " outstanding violations for " + carName);
                        mViolationsAdapter.setViolationsList(violationsList);
                    }

                    @Override
                    public void onFailure(Call<List<ParkingCameraResponse>> call, Throwable t) {
                        Toast.makeText(getContext(), "Unable to Download Data", Toast.LENGTH_SHORT).show();
                    }
                });
        ;
    }

    private List<ParkingCameraResponse> parseResponseForOutstandingViolations(List<ParkingCameraResponse> body) {
        List<ParkingCameraResponse> responseViolations = new ArrayList<>();

        for (int i = 0; i < body.size(); i++) {
            ParkingCameraResponse response = body.get(i);
            if(response.getAmountDue() != 0){
                responseViolations.add(response);
            }
        }
        return responseViolations;
    }
}
