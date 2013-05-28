package com.nemogames;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.facebook.FacebookException;
import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.Request.Callback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.Session.OpenRequest;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

public class FAgentActivity implements Session.StatusCallback, NemoActivityListener
{
	public static	String			Permission_Publish = "publish_actions";
	
	public	void	init(final String gameobject, final String functionname)
	{
		Log.d("Nemo - FacebookAgent", "Starting Facebook Agent Activity");
		this.ListenerFunction = functionname;
		this.ListenerGameObject = gameobject;
		RootActivity = NemoActivity.instance();
		RootActivity.RegisterActivityListener(this);
	}

	private String				ListenerGameObject = "";
	private String				ListenerFunction = "";
	private GraphUser			LoggedInUser;
	private ArrayList<GraphUser>	Friends;
	private NemoActivity		RootActivity;
	
    public boolean		hasPublishPermission()
    {
    	return this.hasPermission(Permission_Publish);
    }
    
    public boolean		hasPermission(String permission)
    {
    	if (Session.getActiveSession() == null || !Session.getActiveSession().isOpened()) return false;
    	return Session.getActiveSession().getPermissions().contains(permission);
    }
    
    public boolean		isLoggedIn()
    {
    	return Session.getActiveSession().isOpened();
    }
    
    public String		getCurrentUserBasicInfo()
    {
    	return this.getGraphUserBasicInfo(LoggedInUser);
    }
    
    public String		getGraphUserBasicInfo(GraphUser user)
    {
    	String jsonData = "";
    	try {
    		JSONObject data = new JSONObject();
    		data.put("FirstName", user.getFirstName());
    		data.put("LastName", user.getLastName());
    		data.put("City", user.getLocation().getCity());
    		data.put("Country", user.getLocation().getCountry());
    		data.put("ID", user.getId());
    		data.put("Birthday", user.getBirthday());
    		data.put("Gender", user.getProperty("Gender"));
    		jsonData = data.toString();
    	} catch (JSONException e) { e.printStackTrace(); }
    	return jsonData;
    }
    
    public String		getFriendsBasicInfo()
    {
    	return "";
    }
    
    public String		getSessionState()
    {
    	if (Session.getActiveSession() != null)
    		return Session.getActiveSession().getState().toString();
    	else return "Null";
    }
    
    public String		getAccessToken()
    {
    	if (Session.getActiveSession() != null)
    		return Session.getActiveSession().getAccessToken();
    	else return "";
    }
    
    public void			Login()
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
			@Override
			public void run() 
			{
				Session.getActiveSession().openForRead(new OpenRequest(RootActivity));
			}
    	});
    }
    
    public void			RequestNewPublishPermission(final String jsonarray)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			try
		    	{
		    		JSONArray array = new JSONArray(jsonarray);
		        	List<String> permission = new ArrayList<String>();
		        	for (int i = 0; i < array.length(); i++) permission.add(array.getString(i));
		    		Session.getActiveSession().requestNewPublishPermissions(new NewPermissionsRequest(RootActivity, permission));
		    	} catch (JSONException e) { e.printStackTrace(); }
    		}
    	});
    }
	
    public void			RequestNewReadPermission(final String jsonarray)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
		    	try
		    	{
		    		JSONArray array = new JSONArray(jsonarray);
		        	List<String> permission = new ArrayList<String>();
		        	for (int i = 0; i < array.length(); i++) permission.add(array.getString(i));
		    		Session.getActiveSession().requestNewReadPermissions(new NewPermissionsRequest(RootActivity, permission));
		    	} catch (JSONException e) { e.printStackTrace(); }
			}
    	});
    }
    
    public void			FetchGraphUser()
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
				Request.executeMeRequestAsync(Session.getActiveSession(), new Request.GraphUserCallback() 
		    	{
					@Override
					public void onCompleted(GraphUser user, Response response) 
					{
						FAgentActivity.this.LoggedInUser = user;
						Log.d("Nemo - FacebookAgent", "FetchGraphUser Respone: " + response.toString());
					}
				});
			}
    	});
    }
    
    public void			Logout()
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			Session.getActiveSession().close();
			}
    	});
    }
    
    public void			FetchUserFriends()
    {
    	
    }
    
    public void			ShowFeedDialog(final Bundle params)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			Log.d("Nemo - FacebookAgent", "Feeding: " + params.toString());
    			WebDialog feed = (new WebDialog.FeedDialogBuilder(RootActivity, Session.getActiveSession(), params))
				.setOnCompleteListener(new OnCompleteListener()
				{
					@Override
					public void onComplete(Bundle values,
							FacebookException error) 
					{
						if (error != null) 
							Log.e("Nemo - FacebookAgent", error.getMessage());
						else
							Log.d("Nemo - FacebookAgent", "Feed Dailog: values=" + values.toString());
						
					}
				}).build();
    			feed.show();
			}
    	});
    }
    
    public void			UploadPhotoRequest(final byte[] data)
    {

    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
		    	Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
		    	Request upload = Request.newUploadPhotoRequest(Session.getActiveSession(), image, new Callback()
		    	{
					@Override
					public void onCompleted(Response response) 
					{
						Log.d("Nemo - Facebook Agent","Done. uploading: " + response.toString());
					}
		    	});
		    	upload.getParameters().putString("name", "Name Goes Here");
		    	upload.getParameters().putString("caption", "Caption Goes Here");
		    	upload.getParameters().putString("link", "http://www.nemo-games.com");
		    	upload.getParameters().putString("message", "my message goes here");
		    	Log.d("Nemo - Facebook Agent", "Bundle: " + upload.getParameters().toString());
		    	upload.executeAsync();
			}
    	});
    }
    
    public void			GraphPathRequest(final String path)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
		    	Request.executeGraphPathRequestAsync(Session.getActiveSession(), path, new Callback()
		    	{
					@Override
					public void onCompleted(Response response) 
					{
					}
		    	});
			}
    	});
    }
    
    public void			StatusUpdateRequest(final String message)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
		    	Request.executeStatusUpdateRequestAsync(Session.getActiveSession(), message, new Callback()
		    	{
					@Override
					public void onCompleted(Response response) 
					{
					}
		    	});
			}
    	});
    }
    
    public void			UploadPhotoRequest(final String path)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
		    	try 
		    	{
					Request.executeUploadPhotoRequestAsync(Session.getActiveSession(), new File(path), new Callback()
					{
						@Override
						public void onCompleted(Response response) 
						{
						}
					});
					Request r = new Request();
				} catch (FileNotFoundException e) { e.printStackTrace();}
			}
    	});
    }
    
    public void			ShowWebDialog(final String action, final Bundle params)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			WebDialog feed = (new WebDialog.Builder(RootActivity, Session.getActiveSession(), action, params))
				.setOnCompleteListener(new OnCompleteListener()
				{
					@Override
					public void onComplete(Bundle values,
							FacebookException error) 
					{
						Log.d("Nemo - FacebookAgent", "Feed Dailog: values=" + values.toString());
						if (error != null) Log.e("Nemo - FacebookAgent", error.getMessage());
					}
				}).build();
    			feed.show();
			}
    	});
    }
	//------------------------------------------------- private
    
    
    
	//------------------------------------------------- interface
	@Override
    public void onRegistered(Bundle savedInstanceState)
	{
    	try
    	{
            PackageInfo info = RootActivity.getPackageManager().getPackageInfo(RootActivity.getPackageName(), PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) 
            {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("Nemo - FacebookAgent", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (NameNotFoundException e) {} catch (NoSuchAlgorithmException e) {}
    	Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
    	Session session = Session.getActiveSession();
    	if (session == null)
    	{
    		if (savedInstanceState != null)
    		{
    			session = Session.restoreSession(RootActivity, null, this, savedInstanceState);
    		}
    		if (session == null)
    		{
    			session = new Session(RootActivity);
    		}
    		Session.setActiveSession(session);
    		if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED))
    		{
    			session.openForRead(new Session.OpenRequest(RootActivity).setCallback(this));
    		}
    	}
	}

    @Override
    public void onStart() 
    {
        Session.getActiveSession().addCallback(this);
    }

    @Override
    public void onStop() 
    {
        Session.getActiveSession().removeCallback(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        Session.getActiveSession().onActivityResult(RootActivity, requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) 
    {
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }

	@Override
	public void onRestart() 
	{
	}

	@Override
	public void onPause() 
	{
	}

	@Override
	public void onResume() 
	{
	}

	@Override
	public void onDestroy() 
	{
	}

	@Override
	public void onBackPressed() 
	{
	}
	
	//--------------- session callback 
	@Override
	public void call(Session session, SessionState state, Exception exception) 
	{
		if (state != null)
			Log.d("Nemo - FacebookAgent", "Session Callback: state="+state.toString());
		else
			Log.d("Nemo - FacebookAgent", "Session Callback: State is still null");
	}
}
