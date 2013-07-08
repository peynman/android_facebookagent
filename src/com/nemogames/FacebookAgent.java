package com.nemogames;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.Session.OpenRequest;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;
import com.unity3d.player.UnityPlayer;

public class FacebookAgent implements Session.StatusCallback, NemoActivityListener
{
	public static	String			Permission_Publish = "publish_actions";
	
	public enum FacebookEvent
	{
		OnSessionOpenSuccess(1),
		OnSessionOpenFailed(2),
		OnDialogSuccess(3),
		OnDialogFailed(4),
		OnDialogCanceled(5),
		OnRequestSuccess(6),
		OnRequestFailed(7);
		
		int val;
		FacebookEvent(int v) { val = v; }
		public int getValue() { return val; }
	}
	
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
	private List<GraphUser>		Friends;
	private NemoActivity		RootActivity;

    //------------------------------------------------------------ Agent Getters
    public boolean		hasPublishPermission()
    {
    	return this.hasPermission(Permission_Publish);
    }
    
    public boolean		hasPermission(String permission)
    {
    	if (Session.getActiveSession() == null || !Session.getActiveSession().isOpened()) return false;
    	return Session.getActiveSession().getPermissions().contains(permission);
    }
    
    public boolean		isSessionOpened()
    {
    	return Session.getActiveSession().isOpened();
    }
    
    public String		getCurrentUserBasicInfo()
    {
    	return this.getGraphUserBasicInfo(LoggedInUser);
    }
    public boolean		hasCurrentUserBasicInfo()
    {
    	return LoggedInUser != null;
    }
    public boolean		hasCurrentUserFriends()
    {
    	return Friends != null;
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
    	else return "Null";
    }
    //------------------------------------------------------------ Facebook Session
    public void			OpenForRead(final int iid, final String[] permissions)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
			@Override
			public void run()
			{
				if (Session.getActiveSession().isClosed())
				{
					Session newsession = new Session(RootActivity);
					Session.setActiveSession(newsession);
				}
				OpenRequest or = new OpenRequest(RootActivity);
				or.setPermissions(Arrays.asList(permissions));
				or.setCallback(new StatusCallback()
				{
					@Override
					public void call(Session session, SessionState state, Exception exception) 
					{
						FacebookAgent.this.SendSessionEvent(iid, session, state, exception);
					}
				});
				Session.getActiveSession().openForRead(or);
			}
    	});
    }
    public void			OpenForPublish(final int iid, final String[] permissions)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
			@Override
			public void run() 
			{
				if (Session.getActiveSession().isClosed())
				{
					Session newsession = new Session(RootActivity);
					Session.setActiveSession(newsession);
				}
				OpenRequest or = new OpenRequest(RootActivity);
				or.setPermissions(Arrays.asList(permissions));
				or.setCallback(new StatusCallback()
				{
					@Override
					public void call(Session session, SessionState state, Exception exception) 
					{
						FacebookAgent.this.SendSessionEvent(iid, session, state, exception);
					}
				});
				Session.getActiveSession().openForPublish(or);
			}
    	});
    }
    public void			RequestNewPublishPermission(final int iid, final String[] permissions)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			NewPermissionsRequest npr = new NewPermissionsRequest(RootActivity, Arrays.asList(permissions));
    			npr.setCallback(new StatusCallback()
				{
					@Override
					public void call(Session session, SessionState state, Exception exception) 
					{
						FacebookAgent.this.SendSessionEvent(iid, session, state, exception);
					}
				});
	    		Session.getActiveSession().requestNewPublishPermissions(npr);
    		}
    	});
    }
    public void			RequestNewReadPermission(final int iid, final String[] permissions)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			NewPermissionsRequest npr = new NewPermissionsRequest(RootActivity, Arrays.asList(permissions));
    			npr.setCallback(new StatusCallback()
				{
					@Override
					public void call(Session session, SessionState state, Exception exception) 
					{
						FacebookAgent.this.SendSessionEvent(iid, session, state, exception);
					}
				});
		    	Session.getActiveSession().requestNewReadPermissions(npr);
			}
    	});
    }
    public void			Close()
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
    public void			CloseAndClear()
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			Session.getActiveSession().closeAndClearTokenInformation();
			}
    	});
    }
    //------------------------------------------------------------ Facebook Dialogs
    public void			ShowFeedDialog(final int iid, final Bundle params)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			WebDialog feed = (new WebDialog.FeedDialogBuilder(RootActivity, Session.getActiveSession(), params))
				.setOnCompleteListener(new OnCompleteListener()
				{
					@Override
					public void onComplete(Bundle values, FacebookException error) 
					{						
						FacebookAgent.this.SendDialogCompleteEvent(iid, values, error);
					}
				}).build();
    			feed.show();
			}
    	});
    }
    //------------------------------------------------------------ Facebook Requests
    public void			ExecuteMyFriendsRequest(final int iid)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			Request.executeMyFriendsRequestAsync(Session.getActiveSession(), new GraphUserListCallback()
    			{
    				@Override
					public void onCompleted(List<GraphUser> users, Response response) 
					{
    					FacebookAgent.this.Friends = users;
    					FacebookAgent.this.SendRequestCompleteEvent(iid, response);
					}
    			});
			}
    	});
    }
    public void			ExecuteMeRequest(final int iid)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
				Request.executeMeRequestAsync(Session.getActiveSession(),
				new Request.GraphUserCallback() 
		    	{
					@Override
					public void onCompleted(GraphUser user, Response response) 
					{
						FacebookAgent.this.LoggedInUser = user;
						FacebookAgent.this.SendRequestCompleteEvent(iid, response);
					}
				});
			}
    	});
    }
    public void			ExecuteUploadPhotoRequest(final int iid, final String message, final byte[] data)
    {

    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
    			Log.d("Nemo - FacebookAgent", "Uploading photo");
		    	Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
		    	Request upload = Request.newUploadPhotoRequest(Session.getActiveSession(), image,
		    	new Callback()
		    	{
					@Override
					public void onCompleted(Response response) 
					{
						FacebookAgent.this.SendRequestCompleteEvent(iid, response);
					}
		    	});
		    	upload.getParameters().putString("name", message);
		    	upload.executeAsync();
			}
    	});
    }
    public void			ExecuteUploadPhotoRequest(final int iid, final String name, final String filename)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
		    	try 
		    	{
		    		Request r = Request.newUploadPhotoRequest(Session.getActiveSession(), new File(filename),
					new Callback()
					{
						@Override
						public void onCompleted(Response response) 
						{
							FacebookAgent.this.SendRequestCompleteEvent(iid, response);
						}
					});
		    		r.getParameters().putString("name", name);
		    		r.executeAsync();
				} catch (FileNotFoundException e) { e.printStackTrace();}
			}
    	});
    }
    public void			ExecuteGraphPathRequest(final int iid, final String path)
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
						FacebookAgent.this.SendRequestCompleteEvent(iid, response);
					}
		    	});
			}
    	});
    }
    public void			ExecuteStatusUpdateRequest(final int iid, final Bundle params)
    {
    	RootActivity.runOnUiThread(new Runnable() 
    	{
    		@Override
			public void run() 
			{
		    	Request r = Request.newStatusUpdateRequest(Session.getActiveSession(), "", new Callback()
		    	{
					@Override
					public void onCompleted(Response response) 
					{
						FacebookAgent.this.SendRequestCompleteEvent(iid, response);
					}
		    	});
		    	r.setParameters(params);
		    	r.executeAsync();
			}
    	});
    }
	//------------------------------------------------- Send events to unity3d
    private void		SendDialogCompleteEvent(int iid, Bundle values, FacebookException error)
    {
    	try
		{
			JSONObject obj = new JSONObject();
			FacebookEvent dialogevent = FacebookEvent.OnDialogCanceled;
			if (error != null)
			{
				dialogevent = FacebookEvent.OnDialogFailed;
				obj.put("error", error.getMessage());
			} else
			{
				if (values.containsKey("post_id"))
					dialogevent = FacebookEvent.OnDialogSuccess;
				else
					dialogevent = FacebookEvent.OnDialogCanceled;
			}
			obj.put("iid", iid);
			obj.put("eid", dialogevent.getValue());
			this.SendUnity3DEvent(obj.toString());
		} catch (JSONException e) { e.printStackTrace(); }
    }
    private void		SendRequestCompleteEvent(int iid, Response response)
    {
    	try
    	{
			JSONObject obj = new JSONObject();
			obj.put("iid", iid);
			if (response.getError() != null)
			{
				obj.put("eid", FacebookEvent.OnRequestFailed.getValue());
				obj.put("error", response.getError().getErrorMessage());
			} else
			{
				obj.put("eid", FacebookEvent.OnRequestSuccess.getValue());
			}
			this.SendUnity3DEvent(obj.toString());
    	} catch (JSONException e) { e.printStackTrace(); }
    	Log.d("Nemo - FacebookAgent", response.toString());
    }
    private void		SendSessionEvent(int iid, Session session, SessionState state, Exception exception)
    {
    	try
		{
			JSONObject obj = new JSONObject();
			obj.put("iid", iid);
			if (session == null || !session.isOpened())
			{
				obj.put("eid", FacebookEvent.OnSessionOpenFailed.getValue());
				if (exception != null)
					obj.put("error", exception.getMessage());
				else
					obj.put("error", state.toString());
			} else
			{
				 if (session.isOpened())
					obj.put("eid", FacebookEvent.OnSessionOpenSuccess.getValue());
			}
			this.SendUnity3DEvent(obj.toString());
		} catch (JSONException e) { e.printStackTrace(); }
    }
    private void		SendUnity3DEvent(String json)
    {
    	UnityPlayer.UnitySendMessage(ListenerGameObject, ListenerFunction, json);
    }
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
	}
}
