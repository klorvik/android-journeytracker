package utility;

import android.util.Log;
import de.greenrobot.daogenerator.*;

/**
 * Created by lorre on 6/10/14.
 */
public class GenerateDao {

    public static void main(String[] args){
        //GreenDAO
        Schema schema = new Schema(1, "au.edu.uow.journeytracker");

        //Journey
        Entity journeyEntity = schema.addEntity("Journey");
        journeyEntity.addIdProperty();
        journeyEntity.addStringProperty("name").notNull();
        journeyEntity.addDoubleProperty("distance");
        journeyEntity.addDateProperty("createdAt");

        //Coordinate
        Entity coordinateEntity = schema.addEntity("Coordinate");
        coordinateEntity.addIdProperty();
        coordinateEntity.addDoubleProperty("latitude").notNull();
        coordinateEntity.addDoubleProperty("longitude").notNull();
        coordinateEntity.addDoubleProperty("elevation");
        coordinateEntity.addDateProperty("createdAt");

        //Photo
        Entity photoEntity = schema.addEntity("Photo");
        photoEntity.addIdProperty();
        photoEntity.addStringProperty("annotation");
        photoEntity.addByteArrayProperty("image").notNull();
        photoEntity.addDateProperty("createdAt");

        //Relationships
        Property journeyId = coordinateEntity.addLongProperty("journeyId").notNull().getProperty();
        ToMany journeyToCoordinates = journeyEntity.addToMany(coordinateEntity, journeyId);
        journeyToCoordinates.setName("coordinates"); // Optional

        Property coordinateId = photoEntity.addLongProperty("coordinateId").notNull().getProperty();
        ToMany coordinateToPhotos = coordinateEntity.addToMany(photoEntity, coordinateId);
        coordinateToPhotos.setName("photos");

        //Trigger generation
        try {
            DaoGenerator daoGenerator = new DaoGenerator();
            daoGenerator.generateAll(schema, "../JourneyTracker/app/src/main/java/dao");
        } catch (Exception e){
            //Log.v("EX", e.getLocalizedMessage());
        }
    }
}
