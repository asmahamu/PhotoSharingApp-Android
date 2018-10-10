package com.emergent.photosharingapp

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.arch.paging.PagedList
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.emergent.photosharingapp.api.MediaSharingApi
import com.emergent.photosharingapp.domain.Media
import com.emergent.photosharingapp.repository.MediaRepository
import com.emergent.photosharingapp.repository.NetworkState
import com.emergent.photosharingapp.ui.MediaMasterViewModel
import com.emergent.photosharingapp.ui.MediaRecyclerAdapter
import kotlinx.android.synthetic.main.activity_media_master.*
import kotlinx.android.synthetic.main.content_media_master.*

class MediaMasterActivity : AppCompatActivity() {
    private lateinit var mediaMasterViewModel:MediaMasterViewModel
    private var mediaSharingApi = MediaSharingApi.create()
    private var mediaRepository = com.emergent.photosharingapp.repository.MediaRepositoryImpl(mediaSharingApi)
    private lateinit var adapter : MediaRecyclerAdapter
    private val REQ_READ_EXT_STORAGE_PERM: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_master)

        mediaMasterViewModel = getViewModel(mediaRepository)

        initAdapter()
        initSwipeToRefresh()
        setSupportActionBar(toolbar)
        fab.setOnClickListener {
            val permissionFromReadStorage = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED)
            if(permissionFromReadStorage) {
                // Permission already granted
                mediaMasterViewModel.fabClicked(mediaMasterViewModel.REQ_CODE_GALLERY_IMAGE_CAPTURE, this)
            } else{
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.READ_EXTERNAL_STORAGE)){
                    // Show why do you need permission
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(R.string.read_perm_request_dialog_msg)
                            .setTitle(R.string.read_perm_request_dialog_title)

                    builder.setPositiveButton("OK"
                    ) { dialog, id ->
                        makeRequest()
                    }

                    val dialog = builder.create()
                    dialog.show()
                } else{
                    // Get permission
                    makeRequest()
                }
            }
        }
        mediaMasterViewModel.setUserId("1")
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQ_READ_EXT_STORAGE_PERM)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_media_master, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
    @Suppress("UNCHECKED_CAST")
    private fun getViewModel(mediaRepository: MediaRepository):MediaMasterViewModel{
        return ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return MediaMasterViewModel(mediaRepository) as T
            }
        }).get(MediaMasterViewModel::class.java)
    }
    private fun initAdapter() {
        val glide = GlideApp.with(this)
        adapter = MediaRecyclerAdapter(glide) {
            mediaMasterViewModel.retry()
        }
        my_recycler_view.adapter = adapter
        my_recycler_view.layoutManager = LinearLayoutManager(this)
        mediaMasterViewModel.posts.observe(this, Observer<PagedList<Media>> {
            adapter.submitList(it)
        })
        mediaMasterViewModel.networkState.observe(this, Observer {
            adapter.setNetworkState(it)
        })
        mediaMasterViewModel.mediaDownloaded.observe(this, Observer {
            Toast.makeText(this, "File upload completed", Toast.LENGTH_SHORT).show()
        })
    }
    private fun initSwipeToRefresh() {
        mediaMasterViewModel.refreshState.observe(this, Observer {
            swipe_refresh.isRefreshing = it == NetworkState.LOADING
        })
        swipe_refresh.setOnRefreshListener {
            mediaMasterViewModel.refresh()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mediaMasterViewModel.onActivityResult(this, requestCode, resultCode, data)
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQ_READ_EXT_STORAGE_PERM -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // read storage permission was granted, yay! Do related task
                    mediaMasterViewModel.fabClicked(mediaMasterViewModel.REQ_CODE_GALLERY_IMAGE_CAPTURE, this)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
