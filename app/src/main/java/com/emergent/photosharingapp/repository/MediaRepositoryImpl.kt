/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.emergent.photosharingapp.repository

import android.arch.lifecycle.Transformations
import android.arch.paging.LivePagedListBuilder
import android.support.annotation.MainThread
import com.emergent.photosharingapp.api.MediaSharingApi
import com.emergent.photosharingapp.domain.Media
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Callback
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Repository implementation that returns a Listing that loads data directly from network by using
 * the previous / next page keys returned in the query.
 */
class MediaRepositoryImpl(private val mediaSharingApi: MediaSharingApi) : MediaRepository {
    private val networkExecutor: Executor = Executors.newFixedThreadPool(5)
    @MainThread
    override fun getMedia(userId: Long, pageSize: Int): Listing<Media> {
        val sourceFactory = MediaDataSourceFactory(mediaSharingApi, userId, networkExecutor)

        val livePagedList = LivePagedListBuilder(sourceFactory, pageSize)
                // provide custom executor for network requests, otherwise it will default to
                // Arch Components' IO pool which is also used for disk access
                .setFetchExecutor(networkExecutor)
                .build()

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        return Listing(
                pagedList = livePagedList,
                networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                  it.networkState
                },
                retry = {
                    sourceFactory.sourceLiveData.value?.retryAllFailed()
                },
                refresh = {
                    sourceFactory.sourceLiveData.value?.invalidate()
                },
                refreshState = refreshState
        )
    }
    override fun uploadMedia(userId: Long, file: File, callback:Callback<Media>) {
        val filePart = MultipartBody.Part.createFormData("file", file.getName(), RequestBody.create(MediaType.parse("image/*"), file))
        val request = mediaSharingApi.uploadMedia(userId, filePart)
        request.enqueue(callback)
    }
    override fun likeMedia(userId: Long, mediaId: Long, callback:Callback<Media>) {
        val request = mediaSharingApi.likeMedia(userId, mediaId)
        request.enqueue(callback)
    }
    override fun unlikeMedia(userId: Long, mediaId: Long, callback:Callback<Media>) {
        val request = mediaSharingApi.unlikeMedia(userId, mediaId)
        request.enqueue(callback)
    }

}

