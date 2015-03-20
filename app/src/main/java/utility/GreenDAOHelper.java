package utility;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import dao.DaoSession;
import entity.Coordinate;
import entity.Journey;
import entity.Photo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lorre on 5/10/14.
 */

public class GreenDAOHelper {

    private DaoSession daoSession;

    public GreenDAOHelper(DaoSession daoSession){
        this.daoSession = daoSession;
    }

    public void saveJourney(Journey journey, List<Coordinate> coordinates, List<Photo> photos){
        Log.v("INFO", " -- DATA SAVED --");
        daoSession.insert(journey);
        for(int i = 0; i < coordinates.size(); i++){
            coordinates.get(i).setJourneyId(journey.getId());
            daoSession.insert(coordinates.get(i));
        }
        for (int i = 0; i < photos.size(); i++) {
            Photo photo = photos.get(i);
            int id = (int)photo.getCoordinateId();
            photo.setCoordinateId(coordinates.get(id).getId());
            daoSession.insert(photos.get(i));
        }
        Log.v("INFO", journey.toString());
        for(Coordinate c : coordinates) Log.v("INFO:", c.toString());
        for(Photo p : photos) Log.v("INFO:", p.toString());
    }

    public void deleteJourney(Journey journey){
        //Method to delete a journey and all objects belonging to the journey
        daoSession.getJourneyDao().delete(journey);
        for(Coordinate c : journey.getCoordinates()){
            daoSession.getCoordinateDao().delete(c);
            for(Photo p : c.getPhotos()){
                daoSession.getPhotoDao().delete(p);
            }
        }
    }
}