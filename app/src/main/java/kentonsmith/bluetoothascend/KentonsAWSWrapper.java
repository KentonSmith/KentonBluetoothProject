package kentonsmith.bluetoothascend;

import android.app.Activity;
import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.regions.Regions;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.*;


/**
 * Created by admin on 12/9/2015.
 */



public class KentonsAWSWrapper {

    private CognitoCachingCredentialsProvider credentialsProvider;
    private AmazonDynamoDBClient ddbClient;
    private DynamoDBMapper mapper;

    //set up AWS cognito
    KentonsAWSWrapper(Context context_this_class_is_used_in)
    {
        //http://docs.aws.amazon.com/mobile/sdkforandroid/developerguide/getting-started-sync-data.html

        // Initialize the Amazon Cognito credentials provider
               credentialsProvider = new CognitoCachingCredentialsProvider(
                context_this_class_is_used_in,    /* get the context for the application */
                "us-east-1:08b24ffc-47a7-457b-88a9-a960f1cf184d",    /* Identity Pool ID */
                Regions.US_EAST_1           /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );


      //  http://docs.aws.amazon.com/mobile/sdkforandroid/developerguide/getting-started-store-query-app-data.html

         ddbClient = new AmazonDynamoDBClient(credentialsProvider);

         mapper = new DynamoDBMapper(ddbClient);



    }

    public void makeRowTest()
    {
        AscendTestTableRow row = new AscendTestTableRow();
        row.setUserId("Kenton Test 1");
        row.setFirstName("Kenton");
        row.setLastName("Smith");

        if(mapper != null)
        {
            mapper.save(row);
        }

    }




}
