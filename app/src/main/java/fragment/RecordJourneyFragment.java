package fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import au.edu.uow.journeytracker.app.MainActivity;
import au.edu.uow.journeytracker.app.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import dao.DaoSession;
import entity.Coordinate;
import entity.Journey;
import entity.Photo;
import utility.GreenDAOHelper;
import utility.ImageConverter;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecordJourneyFragment extends Fragment {


    //Update interval for coordinate saves in MS
    private final int UPDATE_INTERVAL = 10000;

    private MapView mapView;
    private GoogleMap map;
    private ImageButton recordJourneyButton;
    private ImageButton takePhotoButton;

    private DaoSession daoSession;

    private Journey journey;
    private List<Coordinate> coordinates;
    private List<Photo> photos;

    private byte[] currentImage;

    private boolean recording = false;
    private int coordinateId;

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        //Get DAOSESSION
        this.daoSession = ((MainActivity) getActivity()).getDaoSession();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_current_journey, container, false);

        //GUI HOOKUP
        this.recordJourneyButton = (ImageButton) view.findViewById(R.id.recordJourneyButton);
        this.takePhotoButton = (ImageButton) view.findViewById(R.id.takePhotoButton);

        //Config
        takePhotoButton.setVisibility(View.GONE);

        //INIT MAP
        initializeMap(view, savedInstanceState);

        //Set recordbutton listener
        recordJourneyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final RecordJourneyTask task = new RecordJourneyTask();
                if (!recording) {
                    Log.v("INFO", "START RECORDING");

                    //Init variables on new recording
                    journey = new Journey();
                    coordinates = new ArrayList();
                    photos = new ArrayList();

                    recording = true;

                    //Start the asynctask
                    task.execute("");
                    takePhotoButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                         coordinateId = task.getId();
                         savePhoto();
                        }
                    });

                } else {
                    Log.v("INFO", "STOP RECORDING");
                    recording = false;
                    recordJourneyButton.setImageResource(R.drawable.record);
                    takePhotoButton.setVisibility(View.GONE);
                    saveJourney();
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    //Creating asynctask to handle the map updates
    private class RecordJourneyTask extends AsyncTask<String, Integer, Integer> {

        private double totalDistance = 0;
        private int i = 0;
        private int coordinateId = 0;
        private LatLng lastLocation = null;

        @Override
        protected Integer doInBackground(String... dummy) {
              synchronized(this) {
                for(; i < 9999; i++){
                    //Jump out if recording stopped
                    if (!recording) break;

                    if(i>1) {
                        try {
                            Thread.sleep(UPDATE_INTERVAL);
                        } catch (Exception e) {
                            Log.v("Error: ", e.toString());
                        }
                    }
                    publishProgress(i);
                }
                return i;
            }
        }
        public int getId(){
            return coordinateId;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

            //Get location
            Location location = map.getMyLocation();
            LatLng myLocation = null;
            if (location != null) {
                myLocation = new LatLng(location.getLatitude(),
                        location.getLongitude());
            }else{
                //Return if loaction not found
                Toast.makeText(getActivity(), "COULD NOT GET LOCATION", Toast.LENGTH_SHORT).show();
                return;
            }

            //If we have an earlier location
            if(lastLocation != null) {
                if(calculationByDistance(lastLocation, myLocation) >= 5) {
                //Only add coordinate to journey if over 5 meters from the last (no data redundancy)

                    map.addPolyline(new PolylineOptions()
                            .add(
                                    lastLocation,
                                    myLocation
                            )
                            .width(5)
                            .color(Color.RED));

                    // Instantiates a new CircleOptions object and defines the center and radius
                    CircleOptions circleOptions = new CircleOptions()
                            .center(myLocation)
                            .strokeColor(Color.RED)
                            .radius(1); // In meters

                    map.addCircle(circleOptions);

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));
                    totalDistance += calculationByDistance(lastLocation, myLocation);
                    coordinates.add(new Coordinate(null, location.getLatitude(), location.getLongitude(), null, new Date(), 0));
                    coordinateId++;
                    Log.v("INFO", "Distance: " + calculationByDistance(lastLocation, myLocation) + ". Added coordinate");
                }else{
                    Log.v("INFO", "Location too close. I dont bother");
                }
            }else{
                //Add first coordinate no matter what
                CircleOptions circleOptions = new CircleOptions()
                        .center(myLocation)
                        .strokeColor(Color.RED)
                        .radius(1); // In meters
                map.addCircle(circleOptions);
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));

                coordinates.add(new Coordinate(null, location.getLatitude(), location.getLongitude(), null, new Date(), 0));
                coordinateId++;
                Log.v("INFO", "Added first coordinate");
            }

            lastLocation = myLocation;
            journey.setDistance(totalDistance);
        }

        @Override
        protected void onPreExecute() {
            recordJourneyButton.setImageResource(R.drawable.stop);
            takePhotoButton.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Integer result) {
            //Do something after executing
        }
    }

    private void saveJourney(){
        final EditText input = new EditText(getActivity());
        new AlertDialog.Builder(getActivity())
                .setTitle("Save journey")
                .setMessage("Enter name:")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        journey.setName(input.getText().toString());
                        journey.setCreatedAt(new Date());
                        //Saving to database
                        new GreenDAOHelper(daoSession).saveJourney(journey, coordinates, photos);
                        Toast.makeText(getActivity(), "Journey saved to DB", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //Aint doing nuffin'
            }
        }).show();
        map.clear();
    }

    private void savePhoto(){
        if (!hasCamera()) {
            //Pick from gallery
            Intent selectPictureIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(selectPictureIntent, 1);
        }else{
            //Pick from gallery (couldnt test camera, should be pick from camera)
            Intent selectPictureIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(selectPictureIntent, 1);
        }
    }

    //CHECK IF DEVICE HAS CAMERA
    private boolean hasCamera(){
        if (!this.getActivity().getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return false;
        }
        return true;
    }


    /**
     * Photo Selection result
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == 1 || requestCode == 100)  && resultCode == getActivity().RESULT_OK) {
            Uri selectedImage = data.getData();
            InputStream imageStream = null;
            try {
                imageStream = getActivity().getContentResolver().openInputStream(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);
            currentImage = ImageConverter.convertImageToArray(yourSelectedImage);
            Log.v("INFO", "GOT IMAGE");

            final EditText input = new EditText(getActivity());
            new AlertDialog.Builder(getActivity())
                    .setTitle("Save photo")
                    .setMessage("Enter optional annotation:")
                    .setView(input)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Photo photo = new Photo(null, input.getText().toString(), null, new Date(), coordinateId);
                            photo.setImage(currentImage);
                            photos.add(photo);
                            //Add a marker
                            Coordinate coordinate = coordinates.get(coordinateId-1);
                            map.addMarker(new MarkerOptions()
                                    .position(new LatLng(coordinate.getLatitude(), coordinate.getLongitude()))
                                    .title("Photo")
                                    .snippet(photo.getAnnotation())
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.photo_marker)));
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //Aint doing nuffin'
                }
            }).show();
        }
    }

    //GOOGLE MAPS INITIALIZATION
    private void initializeMap(View view, Bundle savedInstanceState){
        // Gets the MapView from the XML layout and creates it
        MapsInitializer.initialize(getActivity());
        switch (GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity()) )
        {
            case ConnectionResult.SUCCESS:
                mapView = (MapView) view.findViewById(R.id.map);
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
                        //Toast.makeText(getActivity(), "COULD NOT GET LOCATION", Toast.LENGTH_SHORT).show();
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

    //Fetched from http://stackoverflow.com/questions/14394366/find-distance-between-two-points-on-map-using-google-map-api-v2
    public double calculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius=6371;//radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return (Radius * c) * 1000;
    }
}
