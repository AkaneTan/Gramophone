/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.logic

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

/**
 * This is a [GlideModule], mainly used to control caching to be appropriate for Gramophone
 */
@GlideModule
class GramophoneGlideModule : AppGlideModule() {
    override fun applyOptions(
        context: Context,
        builder: GlideBuilder,
    ) {
        builder.setLogLevel(Log.ERROR)
        builder.setDefaultRequestOptions(
            RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE).centerCrop())
    }
}
