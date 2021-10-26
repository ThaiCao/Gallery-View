package com.example.galleryview;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bm.library.Info;
import com.bm.library.PhotoView;
import com.example.galleryview.model.GalleryItem;
import com.example.galleryview.presenter.MainActivityInterface;
import com.example.galleryview.presenter.MainActivityPresenter;
import com.example.galleryview.ui.ItemAdapter;
import com.example.galleryview.ui.MyItemTouchHelperCallBack;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends MyActivity implements View.OnClickListener, MainActivityInterface {


    int operationCode = 0;
    Info info;
    FloatingActionButton selectButton, clearAllButton, filterButton;
    DrawerLayout drawerLayout;
    ActivityResultLauncher<Intent> launcher_album;
    RecyclerView recyclerView;
    CoordinatorLayout coordinatorLayout;
    ListView listView;
    ImageView imageView;
    List<String> strings = new ArrayList<>();
    Animation fadeIn, fadeOut;
    FrameLayout background;
    Toolbar toolbar;
    PhotoView photoView;
    MainActivityPresenter presenter;
    StaggeredGridLayoutManager layoutManager;
    public static final String BOOK_TITLE = "Gallery";
    public static final String FILTER_BOOL_TITLE = "Filters";
    private static final String TAG = "MainActivity";
    public static final int SHOW_FULLSCREEN_IMAGE = 1;
    public static final int SHOW_FILTER_CHOOSE_DIALOG = 2;
    public static final int UNDO_REMOVE_IMAGE = -1;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            init();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        launcher_album = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "onActivityResult: ");
                    if (result.getResultCode() == RESULT_OK) {
                        assert result.getData() != null;
                        Uri uri = Uri.parse(result.getData().toUri(Intent.URI_ALLOW_UNSAFE));
                        MainActivityPresenter.addNewImage(uri, operationCode);
                    }
                });
    }

    @Override
    public void insertNewImage(GalleryItem newItem) {
        recyclerView.scrollToPosition(0);
        presenter.getAdapter().addImage(newItem);
    }

    @Override
    protected void onDestroy() {
        presenter = null;
        super.onDestroy();
    }

    @Override
    public void finish() {
        presenter.getHandler().removeCallbacksAndMessages(null);
        presenter = null;
        super.finish();
    }

    @Override
    public void onClick(View v) {
        final int selectButtonID = R.id.button2;
        final int deleteButtonID = R.id.deleteButton;
        final int filterButtonID = R.id.filterButton;
        switch (v.getId()) {
            case selectButtonID:
                operationCode = 1;
                selectImage();
                break;
            /* 上传按钮逻辑已被移除
            case R.id.uploadButton:
                operationCode = 1;
                selectImage();
                break;
                */
            case filterButtonID:
                boolean[] checkedItems = presenter.getShowLabels();
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Choose labels to show").
                        setMultiChoiceItems(presenter.getLabels(), checkedItems, (dialog, which, isChecked) -> {
                        }).setPositiveButton("Apply", (dialog, which) -> {
                            presenter.updateLabels(checkedItems, 0);
                            presenter.reArrangeAdapter(checkedItems);
                        }).setNegativeButton("Cancel", (dialog, which) -> {
                });
                builder.show();
                break;
            case deleteButtonID:
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("Remove All Videos");
                dialog.setMessage("Are you sure to remove all the videos? " + "This operation cannot be withdrawn.");
                dialog.setPositiveButton("Confirm", (dialog1, which) -> {
                    presenter.clearAll();
                    presenter.getAdapter().clearList();
                    Toast.makeText(this, "All images have been removed.", Toast.LENGTH_SHORT).show();
                    clearAllButton.setVisibility(View.INVISIBLE);
                });
                dialog.setNegativeButton("Cancel", (dialog12, which) -> {
                });
                dialog.show();
        }
    }

    private void selectImage() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openAlbum();
        }
    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("*/*");
        launcher_album.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openAlbum();
        } else Toast.makeText(this, "You denied the permission.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            }
        }
        return true;
    }

    private void LoadImages() throws ExecutionException, InterruptedException {
        //presenter.readAlbumDataFromDatabase();
        presenter.readAlbumDataFromRoomDatabase();
    }


    private void init() throws ExecutionException, InterruptedException {
        recyclerView = findViewById(R.id.galleryRecyclerView);
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        presenter = new MainActivityPresenter(this);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.outline_menu_24);
        }
        background = findViewById(R.id.background);
        layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        selectButton = findViewById(R.id.button2);
        filterButton = findViewById(R.id.filterButton);
        photoView = findViewById(R.id.mainFullscreenImage);
        imageView = findViewById(R.id.imageView);
        fadeIn = new AlphaAnimation(0.0f, 0.9f);
        fadeOut = new AlphaAnimation(0.9f, 0.0f);
        coordinatorLayout = findViewById(R.id.CoordinatorLayout);
        clearAllButton = findViewById(R.id.deleteButton);
        selectButton.setOnClickListener(this);
        filterButton.setOnClickListener(this);
        selectButton.setLongClickable(true);
        selectButton.setOnLongClickListener(v -> {
            Snackbar.make(selectButton, "You've long pressed this button.", Snackbar.LENGTH_SHORT).show();
            clearAllButton.setVisibility(View.VISIBLE);
            return true;
        });
        clearAllButton.setOnClickListener(this);
        presenter.setAdapter(new ItemAdapter(presenter.getGalleryItemList(), presenter.getHandler()));
        ItemTouchHelper.Callback callback = new MyItemTouchHelperCallBack(presenter.getAdapter());
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(presenter.getAdapter());
        LoadImages();
        strings.add("Upload this image");
        strings.add("Delete this image");
        fadeIn.setDuration(photoView.getAnimaDuring());
        fadeOut.setDuration(photoView.getAnimaDuring());
    }


    @Override
    public void showUndoRemoveSnackbar(GalleryItem item, int Position) {
        Snackbar.make(selectButton, "Image removed.", Snackbar.LENGTH_SHORT)
                .setAction("Undo", v -> {
                    presenter.getAdapter().insertImage(item, Position);
                    MainActivityPresenter.reAddItem(item);
                    recyclerView.scrollToPosition(Position);
                })
                .show();
    }

    @Override
    public void showFilterChooseDialog(CharSequence[] items, boolean[] checkedItems, long videoID) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Setting Label")
                .setPositiveButton("Apply", (dialog, which) -> {
                    presenter.updateLabels(checkedItems, videoID);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                }).setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                });
        listView = builder.show().getListView();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void showFullscreenPhoto(String path, Info imageInfo) {
        Log.d(TAG, path);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        photoView = findViewById(R.id.mainFullscreenImage);
        info = imageInfo;
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null) {
            photoView.setImageBitmap(bitmap);
            photoView.setAnimaDuring(200);
            photoView.setOnLongClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                Vibrator vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(30);
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
                builder.setItems(strings.toArray(new String[0]), (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(MainActivity.this, "Upload", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            Toast.makeText(MainActivity.this, "Delete", Toast.LENGTH_SHORT).show();
                            break;
                    }
                });
                builder.setCancelable(true);
                builder.show();
                return true;
            });
            photoView.animaFrom(info);
            photoView.enable();
            photoView.setVisibility(View.VISIBLE);
            backgroundChange();
            photoView.setOnClickListener(v -> {
                if (photoView.getVisibility() == View.VISIBLE) {
                    hideFullscreenPhoto();
                }
            });
        } else {
            Toast.makeText(this, "Fail to load image.", Toast.LENGTH_SHORT).show();
        }
    }

    public void hideFullscreenPhoto() {
        photoView.destroyDrawingCache();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        backgroundChange();
        photoView.animaTo(info, () -> photoView.setVisibility(View.INVISIBLE));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (photoView.getVisibility() == View.VISIBLE)
            hideFullscreenPhoto();
        else {
            super.onBackPressed();
        }
    }

    public void backgroundChange() {


        if (background.getVisibility() == View.INVISIBLE) {
            background.setVisibility(View.VISIBLE);
            background.startAnimation(fadeIn);
        } else {
            background.startAnimation(fadeOut);
            background.setVisibility(View.INVISIBLE);
        }
    }


}
