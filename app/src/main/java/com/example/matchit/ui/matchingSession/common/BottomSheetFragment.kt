package com.example.matchit.ui.matchingSession.common

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.matchit.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BottomSheetFragment : BottomSheetDialogFragment() {

    private var recoverableForm: SessionForm? = null

    fun registerRecoverableForm(form: SessionForm) {
        recoverableForm = form
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        recoverableForm?.let {
            if (!it.isValid()) { // last form validation before close was not valid ==> we recover last snapshot
                it.recoverIfNeeded()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_sheet, container, false)
    }

    override fun onStart() {
        super.onStart()
        // Cast dialog to BottomSheetDialog and get the behavior
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels

            val maxHeightPercent = arguments?.getFloat(ARG_MAX_SCREEN_PERCENTAGE_HEIGHT, 0.7f) ?: 0.7f
            behavior.maxHeight = (screenHeight * maxHeightPercent).toInt() // 70% of screen height
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            val childFragmentClassName = arguments?.getString(ARG_CHILD_FRAGMENT_CLASS)
            val childArgs = arguments?.getBundle(ARG_CHILD_FRAGMENT_ARGS)

            if (childFragmentClassName != null) {
                try {
                    val childFragment = (Class.forName(childFragmentClassName)
                        .getConstructor()
                        .newInstance() as Fragment).apply {
                        arguments = childArgs
                    }
                    childFragmentManager.beginTransaction()
                        .replace(R.id.bottomSheetContainer, childFragment)
                        .commit()
                } catch (e: Exception) {
                    e.printStackTrace() // Handle instantiation errors
                }
            }
        }
    }

    companion object {
        private const val ARG_CHILD_FRAGMENT_CLASS = "child_fragment_class"
        private const val ARG_CHILD_FRAGMENT_ARGS = "child_fragment_args"
        private const val ARG_MAX_SCREEN_PERCENTAGE_HEIGHT = "max_height"

        fun newInstance(childFragmentClass: Class<out Fragment>, childArgs: Bundle? = null, maxHeight: Float? = null ): BottomSheetFragment {
            val fragment = BottomSheetFragment()
            val args = Bundle().apply {
                putString(ARG_CHILD_FRAGMENT_CLASS, childFragmentClass.name) // Store class name with key
                childArgs?.let { putBundle(ARG_CHILD_FRAGMENT_ARGS, it) }
                maxHeight?.let { putFloat(ARG_MAX_SCREEN_PERCENTAGE_HEIGHT, it) }
            }

            fragment.arguments = args
            return fragment
        }
    }
}