package fragment;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;
import au.edu.uow.journeytracker.app.R;
import au.edu.uow.journeytracker.app.ViewJourneyActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import dao.CoordinateDao;
import dao.JourneyDao;
import entity.Coordinate;
import entity.Journey;
import entity.Photo;
import utility.ImageConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ViewJourneyFragment extends Fragment {

    private MapView mapView;
    private GoogleMap map;
    private Journey journey;
    private List<Photo> images;

    private int mImageNum;

    private int currentimageindex=0;
    private ImageView slidingimage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Shoudl be passed (not finished)
        this.journey = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_journey_map, container, false);
        slidingimage = (ImageView) view.findViewById(R.id.imageView);
        slidingimage.setImageResource(R.drawable.photo_placeholder);
        //INIT MAP
        initializeMap(view, savedInstanceState);
        //play();
        //Draw journey
        //drawJourney();
        return view;
    }

    private void drawJourney(){
        for(int i = 0; i < journey.getCoordinates().size(); i++) {
            Coordinate c = journey.getCoordinates().get(i);
            map.addPolyline(new PolylineOptions()
                    .add(
                            new LatLng(c.getLatitude(),c.getLongitude()),
                            new LatLng(c.getLatitude(), c.getLongitude())
                    )
                    .width(5)
                    .color(Color.RED));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(c.getLatitude(), c.getLongitude()), 18));
        }
    }



    private void play(){
        images = new ArrayList();
        for (Coordinate c : journey.getCoordinates()) {
            if (!c.getPhotos().isEmpty()) {
                for (Photo p : c.getPhotos()) {
                    images.add(p);
                    Log.v("INFO", "FOUND IMAGE");
                }
            }
        }
            //First, check if selected journey has photos
            boolean hasPhotos = false;
            for (Coordinate c : journey.getCoordinates()) {
                if (!c.getPhotos().isEmpty()) {
                    hasPhotos = true;

                }
            }

            //Dont play if journey got no photos
            if (!hasPhotos) {
                Toast.makeText(getActivity(), "NO PHOTOS!", Toast.LENGTH_SHORT).show();
                return;
            }


        //If it has photos, start playing.
        //Create a new timer that runs the slideshow method in a schedule
        Timer timer = new Timer();
        class SlideShowTask extends TimerTask {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startSlideShow();
                    }
                });
            }
        }
        timer.schedule(new SlideShowTask(), 0, 5 * 1000);
    }

    //Method to start slideshow.
    private void startSlideShow() {

        if(images.size() > 0) {
            Photo currentPhoto = images.get(currentimageindex % images.size());
            slidingimage.setImageBitmap(ImageConverter.convertArrayToImage(currentPhoto.getImage()));
            currentimageindex++;
            Animation rotateimage = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
            slidingimage.startAnimation(rotateimage);
        }else{
            slidingimage.setImageResource(R.drawable.photo_placeholder);
        }
    }

    private void initializeMap(View view, Bundle savedInstanceState){
        // Gets the MapView from the XML layout and creates it
        MapsInitializer.initialize(getActivity());
        switch (GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity()) )
        {
            case ConnectionResult.SUCCESS:
                mapView = (MapView) view.findViewById(R.id.view_journey_map);
                mapView.onCreate(savedInstanceState);
                // Gets to GoogleMap from the MapView and does initialization stuff
                if(mapView!=null)
                {
                    map = mapView.getMap();
                    map.getUiSettings().setMyLocationButtonEnabled(true);
                    map.setMyLocationEnabled(true);

                    Location location = map.getMyLocation();
                    LatLng myLocation = null;
                    if (location != null) {
                        myLocation = new LatLng(location.getLatitude(),
                                location.getLongitude());
                    }else{
                        //Default location if error while fetching (UOW)
                        myLocation = new LatLng(-34.405389, 150.878433);
                    }
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));
                }
                break;
            case ConnectionResult.SERVICE_MISSING:
                Toast.makeText(getActivity(), "SERVICE MISSING", Toast.LENGTH_SHORT).show();
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Toast.makeText(getActivity(), "UPDATE REQUIRED", Toast.LENGTH_SHORT).show();
                break;
            case ConnectionResult.API_UNAVAILABLE:
                Toast.makeText(getActivity(), "API UNAVAILABLE", Toast.LENGTH_SHORT).show();
                break;
            default: Toast.makeText(getActivity(), GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity()), Toast.LENGTH_SHORT).show();
        }
    }


}
