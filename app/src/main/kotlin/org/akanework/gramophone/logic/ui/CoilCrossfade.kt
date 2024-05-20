package org.akanework.gramophone.logic.ui

import androidx.appcompat.content.res.AppCompatResources
import coil3.annotation.ExperimentalCoilApi
import coil3.asCoilImage
import coil3.request.ImageRequest

@OptIn(ExperimentalCoilApi::class)
fun ImageRequest.Builder.placeholderScaleToFit(placeholder: Int) {
	placeholder { ScaleDrawable(AppCompatResources.getDrawable(it.context, placeholder)!!).asCoilImage() }
}