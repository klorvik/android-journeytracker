package fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import au.edu.uow.journeytracker.app.MainActivity;
import au.edu.uow.journeytracker.app.R;

import dao.DaoSession;
import entity.Coordinate;
import entity.Journey;
import entity.Photo;
import utility.ImageConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PlaybackJourneyFragment extends Fragment {

    private DaoSession daoSession;

    private int slidePeriod = 5;

    private int currentImageIndex = 0;
    private ImageView slideShowImageView;
    private TextView imageOverlayTextView;
    private TextView intervalTextView;
    private ImageButton playImageButton;
    private ImageButton stopImageButton;
    private LinearLayout optionsLayout;
    private FrameLayout slideshowLayout;
    private CheckBox showAllJourneysCheckBox;

    private List<Photo> images;
    private List<Journey> journeys;
    private Journey selectedJourney;
    private Timer timer;

    private boolean isPlaying = false;


    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        this.daoSession = ((MainActivity) getActivity()).getDaoSession();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Inflate view
        View view = inflater.inflate(R.layout.fragment_playback, container, false);

        //Init objects
        this.journeys = daoSession.getJourneyDao().queryBuilder().list();
        this.images = new ArrayList();

        //UI HOOKUP
        this.slideShowImageView = (ImageView)view.findViewById(R.id.slideshowImageVIew);
        this.imageOverlayTextView = (TextView) view.findViewById(R.id.imageOverlayTextView);
        this.intervalTextView = (TextView) view.findViewById(R.id.intervalTextView);
        this.playImageButton = (ImageButton) view.findViewById(R.id.playImageButton);
        this.stopImageButton = (ImageButton) view.findViewById(R.id.stopImageButton);
        this.optionsLayout = (LinearLayout) view.findViewById(R.id.optionsLayout);
        this.slideshowLayout = (FrameLayout) view.findViewById(R.id.slideshowLayout);
        this.showAllJourneysCheckBox = (CheckBox) view.findViewById(R.id.showAllJourneysCheckBox);

        final Spinner journeySpinner = (Spinner) view.findViewById(R.id.spinner);
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);

        //UI Config
        intervalTextView.setText("Select slideshow interval - Current: " + slidePeriod + "s");
        seekBar.setProgress(slidePeriod);
        seekBar.setMax(50);

        //Dataadpter setup
        ArrayAdapter<Journey> dataAdapter = new ArrayAdapter<Journey>(getActivity(),
                android.R.layout.simple_spinner_item, journeys);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        journeySpinner.setAdapter(dataAdapter);

        //SET LISTENERS
        journeySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                selectedJourney = (Journey) journeySpinner.getSelectedItem();
                if(selectedJourney != null) {
                    Log.v("INFO", "Start slideshow for " + selectedJourney.getName());
                    images = new ArrayList();
                    for (Coordinate c : selectedJourney.getCoordinates()) {
                        if (!c.getPhotos().isEmpty()) {
                            for (Photo p : c.getPhotos()) {
                                images.add(p);
                                Log.v("INFO", "FOUND IMAGE");
                            }
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress > 0) {
                    slidePeriod = progress;
                    intervalTextView.setText("Select slideshow interval - Current: " + progress + "s");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        stopImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.cancel();
                optionsLayout.setVisibility(View.VISIBLE);
                playImageButton.setVisibility(View.VISIBLE);
                stopImageButton.setVisibility(View.GONE);
                imageOverlayTextView.setVisibility(View.GONE);
                slideShowImageView.setImageBitmap(null);
                isPlaying = false;
            }
        });

        slideshowLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("INFO", "TAPPED");

                if(isPlaying) {
                    if (stopImageButton.getVisibility() == View.GONE) {
                        stopImageButton.setVisibility(View.VISIBLE);
                    } else {
                        stopImageButton.setVisibility(View.GONE);
                    }
                }
            }
        });

        playImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(showAllJourneysCheckBox.isChecked()){
                    images = new ArrayList();
                    for(Journey journey : journeys){
                        for(Coordinate c : journey.getCoordinates()){
                            for(Photo p : c.getPhotos()){
                                images.add(p);
                            }
                        }
                    }
                }else {
                    //First, check if selected journey has photos
                    boolean hasPhotos = false;
                    for (Coordinate c : selectedJourney.getCoordinates()) {
                        if (!c.getPhotos().isEmpty()) {
                            hasPhotos = true;

                        }
                    }

                    //Dont play if journey got no photos
                    if (!hasPhotos) {
                        Toast.makeText(getActivity(), "NO PHOTOS!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                    //If it has photos, start playing.
                    //Create a new timer that runs the slideshow method in a schedule
                    timer = new Timer();
                    playImageButton.setVisibility(View.GONE);
                    optionsLayout.setVisibility(View.GONE);
                    imageOverlayTextView.setVisibility(View.VISIBLE);
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
                    timer.schedule(new SlideShowTask(), 0, slidePeriod * 1000);

                isPlaying = true;
            }
        });

        //Return the view
        return view;
    }

    //Method to start slideshow.
    private void startSlideShow() {

        if(images.size() > 0) {
            Photo currentPhoto = images.get(currentImageIndex % images.size());
            slideShowImageView.setImageBitmap(ImageConverter.convertArrayToImage(currentPhoto.getImage()));
            currentImageIndex++;
            Animation fadeImage = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
            slideShowImageView.startAnimation(fadeImage);
            if(!currentPhoto.getAnnotation().isEmpty()){
                imageOverlayTextView.setText(currentPhoto.getAnnotation() + "\n" + currentPhoto.getCreatedAt().toString());
            }else{
                imageOverlayTextView.setText(currentPhoto.getCreatedAt().toString());
            }
        }else{
            slideShowImageView.setImageResource(R.drawable.photo_placeholder);
            imageOverlayTextView.setText("No images found!");
        }
    }

}
