package net.veierland.aix.data;

import java.io.InputStream;

import net.veierland.aix.AixSettings;
import net.veierland.aix.AixUpdate;
import net.veierland.aix.AixUtils;
import net.veierland.aix.util.AixLocationInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class AixGeoNamesData implements AixDataSource {

	public final static String TAG = "AixGeoNamesData";
	
	private Context mContext;
	private AixSettings mAixSettings;
	private AixUpdate mAixUpdate;
	
	private AixGeoNamesData(Context context, AixUpdate aixUpdate, AixSettings aixSettings)
	{
		mContext = context;
		mAixUpdate = aixUpdate;
		mAixSettings = aixSettings;
	}
	
	public static AixGeoNamesData build(Context context, AixUpdate aixUpdate, AixSettings aixSettings)
	{
		return new AixGeoNamesData(context, aixUpdate, aixSettings);
	}
	
	public void update(AixLocationInfo aixLocationInfo, long currentUtcTime) throws AixDataUpdateException
	{
		String timeZone = aixLocationInfo.getTimeZone();
		String countryCode = mAixSettings.getLocationCountryCode(aixLocationInfo.getId());
		
		mAixUpdate.updateWidgetRemoteViews("Getting timezone data...", false);
		
		if (TextUtils.isEmpty(timeZone) || TextUtils.isEmpty(countryCode))
		{
			String url = String.format(
					"http://api.geonames.org/timezoneJSON?lat=%.5f&lng=%.5f&username=aix_widget",
					aixLocationInfo.getLatitude(),
					aixLocationInfo.getLongitude());
			
			try
			{
				HttpClient httpClient = AixUtils.setupHttpClient();
				HttpGet httpGet = new HttpGet(url);
				HttpResponse response = httpClient.execute(httpGet);
				InputStream content = response.getEntity().getContent();
				
				String input = AixUtils.convertStreamToString(content);
				JSONObject jObject = new JSONObject(input);
				
				timeZone = jObject.getString("timezoneId");
				countryCode = jObject.getString("countryCode");
			}
			catch (Exception e)
			{
				Log.d(TAG, "Failed to retrieve timezone data. (" + e.getMessage() + ")");
				throw new AixDataUpdateException();
			}
			
			mAixSettings.setLocationCountryCode(aixLocationInfo.getId(), countryCode);
			
			aixLocationInfo.setTimeZone(timeZone);
			aixLocationInfo.commit(mContext);
		}
	}
	
}