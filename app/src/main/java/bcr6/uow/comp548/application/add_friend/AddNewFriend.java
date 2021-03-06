package bcr6.uow.comp548.application.add_friend;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.picasso.Picasso;

import bcr6.uow.comp548.application.ImageHelper;
import bcr6.uow.comp548.application.R;
import bcr6.uow.comp548.application.database.DatabaseHelper;
import bcr6.uow.comp548.application.database.ORMBaseActivity;
import bcr6.uow.comp548.application.database.models.Friend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static bcr6.uow.comp548.application.main.MainActivity.API_LEVEL;
import static bcr6.uow.comp548.application.PermissionsHelper.getPermissions;
import static bcr6.uow.comp548.application.PermissionsHelper.hasPermissions;

/**
 * Created by Brendan
 *
 * This activity let's you input details and a photo from the camera or gallery to be saved for a contact
 */
public class AddNewFriend extends ORMBaseActivity<DatabaseHelper> implements AddNewFriendContactPictureFragment.OnFragmentInteractionListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_SELECT = 2;
	public static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 3;
	private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 4;

    private String imagePath = "";
	private String tempPath = "";
	private LatLng loc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_friend_activity);

        //Sets up the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        setTitle(R.string.add_new_friend_toolbar_label);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Log.d("t", "Building picture fragment");

            AddNewFriendContactPictureFragment pictureFragment = AddNewFriendContactPictureFragment.newInstance();
            AddNewFriendDetailsFragment detailsFragment = AddNewFriendDetailsFragment.newInstance();

            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.add_new_contact_picture_container, pictureFragment);
            fragmentTransaction.replace(R.id.add_new_contact_details_container, detailsFragment, "TAG");
            fragmentTransaction.commit();

        } else if (savedInstanceState.containsKey("image")) {
	        imagePath = savedInstanceState.getString("image");
	        tempPath = savedInstanceState.getString("tempPath");
        }

    }

    @Override
    public void onStart() {
	    EditText address = (EditText) findViewById(R.id.add_new_friend_details_address_edit_text);
	    address.setOnFocusChangeListener(new View.OnFocusChangeListener() {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus) {
			    if (hasFocus) {
				    try {
					    Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build((Activity)v.getContext());
					    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
					    v.clearFocus();
				    } catch (GooglePlayServicesRepairableException e) {
					    e.printStackTrace();
				    } catch (GooglePlayServicesNotAvailableException e) {
					    Toast.makeText(v.getContext(), "Unable to contact Google", Toast.LENGTH_LONG).show();
					    e.printStackTrace();
				    }
			    }
		    }
	    });
	    super.onStart();
    }


    /**
     * Saves a toggle into the savedInstanceState, just so we know that this
     * @param out The bundle that we are saving in to. This is the bundle that should get passed in to onCreate
     */
    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putString("image", imagePath);
	    out.putString("tempPath", tempPath);
        super.onSaveInstanceState(out);
    }

    /**
     * Use by my fragment as a click listener to give control back to activity when the fragment is interacted with
     * This particular method deals with selecting photos
     */
    @Override
    public void onFragmentInteraction() {

	    if (API_LEVEL >= 23) {
		    List<String> neededPermissions = hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		    //If there are permissions to request
		    if (!neededPermissions.isEmpty()) {
			    getPermissions(this, WRITE_EXTERNAL_STORAGE_REQUEST_CODE, neededPermissions);
		    } else {
			    photoDialog();
		    }
	    }

    }

    /**
     * Inflates the custom menu items in to the menu on the toolbar
     * @param menu The menu to inflate my items in to
     * @return True if we were able to inflate it
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_new_friend_action_items, menu);
        return true;
    }

    /**
     *
     * @param item the item selected within the menu
     * @return ..
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_contact: {
                if (insertFriend())
                    NavUtils.navigateUpFromSameTask(this);
                return true;
            }
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This is called when an activity is called with an intent to return with a result.
     * @param requestCode The (hopefully) unique code that got sent with the intent
     * @param resultCode Successful or not code
     * @param data The data returned from the intent
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

	    if (resultCode == RESULT_OK) {

		    if (requestCode == REQUEST_IMAGE_CAPTURE) {
				imagePath = tempPath;

			} else if (requestCode == REQUEST_IMAGE_SELECT) {
			    try {
				    InputStream is;
				    is = getContentResolver().openInputStream(data.getData());

				    if (is != null) {
					    imagePath = ImageHelper.createImageFile(this);
					    if (imagePath != null) {
						    OutputStream fos = new FileOutputStream(imagePath);

						    byte[] buffer = new byte[65536];
						    int len;

						    while ((len = is.read(buffer)) != -1)
							    fos.write(buffer, 0, len);

						    fos.close();
						    is.close();
					    } else
						    throw new IOException("Error selecting photo");
				    }

			    } catch (Exception e) {
				    e.printStackTrace();
			    }
		    } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
			    Place place = PlaceAutocomplete.getPlace(this, data);

			    loc = place.getLatLng();

			    TextView textView = (TextView) findViewById(R.id.add_new_friend_details_address_edit_text);
			    textView.setText(place.getAddress());

			    Log.i("Place", "Place: " + place.getName());
		    }

		    ImageView iV = ((ImageView) findViewById(R.id.add_new_friend_picture_silhouette));
		    iV.setScaleType(ImageView.ScaleType.CENTER_CROP);
			Picasso.with(this).load("file://"+imagePath).resize(400, 400).into(iV);


	    } else {
		    if (requestCode == REQUEST_IMAGE_CAPTURE) {
			    File f = new File(tempPath);
			    if (!f.delete())
				    Log.e("Image", "Unable to delete empty image that we were going to use for the photo");
		    } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {

				Status status = PlaceAutocomplete.getStatus(this, data);
				Log.i("Place", status.getStatusMessage());
			}
	    }
    }

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

		if (permissions.length > 0 && grantResults.length > 0) { //If the permissions request was not interrupted

			if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {

				for (int i : grantResults) {
					if (i == PackageManager.PERMISSION_DENIED) {
						Toast.makeText(this, "This feature requires the requested permissions. It will not run without them.", Toast.LENGTH_LONG).show();
						Log.e("Permissions", "Denied permission; " +permissions[i] + ". Feature is disabled as a result");
						return;
					}
				}
				photoDialog();
			}
		}
	}

    /**
     * Adds a friend to the database
     * @return True if successfully added
     */
    private boolean insertFriend() {
        EditText firstNameText = (EditText) findViewById(R.id.add_new_friend_details_first_name_edit_text);
        EditText lastNameText = (EditText) findViewById(R.id.add_new_friend_details_last_name_edit_text);
        EditText mobileNumberText = (EditText) findViewById(R.id.add_new_friend_details_mobile_number_edit_text);
        EditText emailAddressText = (EditText) findViewById(R.id.add_new_friend_details_email_edit_text);
        EditText addressText = (EditText) findViewById(R.id.add_new_friend_details_address_edit_text);


        String firstName = firstNameText.getText().toString();
        String lastName = lastNameText.getText().toString();
        String mobileNumber = mobileNumberText.getText().toString();
        String emailAddress = emailAddressText.getText().toString();
        String address = addressText.getText().toString();

        //Needs at least a name
        if (firstName.isEmpty() && lastName.isEmpty()) {
            firstNameText.setError("");
            lastNameText.setError("");
            return false;
        }

        Friend friend = new Friend(
                firstName,
                lastName,
                mobileNumber,
                emailAddress,
                address,
		        imagePath,
		        loc);

        //Adds the friend to the database
        getHelper().getFriendDataDao().create(friend);

        return true;
    }

    private void photoDialog() {
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    final String[] items = getResources().getStringArray(imagePath.isEmpty() ? R.array.no_photo : R.array.new_photo);
	    builder.setTitle("Change photo")
			    .setItems(items, new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
					    switch (items[which]) {
						    case "Take Photo":
						    case "Take new photo":
							    takePhoto();
							    break;
						    case "Select new photo":
						    case "Choose photo":
							    choosePhoto();
							    break;
						    case "Remove photo":
							    removePhoto();
							    break;
					    }
				    }
			    })
			    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
					    //Mandatory onClick override. Don't need to do anything here
				    }
			    }).create().show();
    }

    /**
     * Let's you choose a photo from the gallery to use as the contact photo
     */
    private void choosePhoto() {
        Intent intent = new Intent(
		        Intent.ACTION_PICK,
		        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        startActivityForResult(intent, REQUEST_IMAGE_SELECT);
    }

    /**
     * Uses the phone's camera to take a photo
     */
    private void takePhoto() {
	    if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {

		    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
			    String fileName = "JPEG_" + timeStamp;

			    File internalDirectory = new File(getFilesDir(), "images");
			    if (!internalDirectory.mkdirs())
				    Log.e("Image", "Image directory either already exists... or it was unable to be created (uh-oh)");

			    File image = null;

			    try {
				    image = File.createTempFile(fileName, ".jpg", internalDirectory);
			    } catch (IOException e) {
				    e.printStackTrace();
			    }

			    if (image != null) {
				    Uri photoURI = FileProvider.getUriForFile(this, "bcr6.uow.comp548.application.fileprovider", image);
				    tempPath = image.getAbsolutePath(); //creates a temporary file, saving path in imagePath
				    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
				    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
			    }
		    }
	    }
    }


    /**
     * Removes the user's photo and sets it back to the default one
     */
    private void removePhoto() {
        ImageView iV = ((ImageView) findViewById(R.id.add_new_friend_picture_silhouette));
        iV.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iV.setImageResource(R.drawable.ic_person_white_24dp);
        imagePath = "";
    }

    /**
     * @return The current image path to the image in the image view. Will be and empty String if no image
     */
    public String getImagePath() {
        return imagePath;
    }

}
