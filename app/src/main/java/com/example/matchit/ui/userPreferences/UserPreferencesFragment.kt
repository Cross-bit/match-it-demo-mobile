package com.example.matchit.ui.userPreferences

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.matchit.R
import com.example.matchit.databinding.FragmentUserPreferencesBinding
import com.example.matchit.ui.launch.LaunchActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileInputStream
import android.content.Intent
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File


@AndroidEntryPoint
class UserPreferencesFragment : Fragment() {

    val profilePicMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

        if (uri != null) {
            this.uploadProfilePicture(uri)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    private fun uploadProfilePicture(uri: Uri) {

        try {

            val mimeType = requireContext().contentResolver.getType(uri)

            if (mimeType == null) {
                Toast.makeText(context, R.string.invalid_file_type_error, Toast.LENGTH_SHORT).show()
                return;
            }

            if (uri.path == null) {
                Toast.makeText(context, R.string.something_error, Toast.LENGTH_SHORT).show()
                return;
            }

            val fileName = File(uri.path).name

            if (fileName.isEmpty()) {
                Toast.makeText(context, R.string.something_error, Toast.LENGTH_SHORT).show()
                return;
            }

            val parcelFileDescriptor = requireContext().contentResolver.openFileDescriptor(uri, "r", null)

            parcelFileDescriptor?.use { pfd ->
                val inputStream = FileInputStream(pfd.fileDescriptor)
                userPreferenceViewModel.uploadProfilePicture(inputStream, mimeType, fileName)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "File Error", Toast.LENGTH_SHORT).show()
        }
    }

    private val userPreferenceViewModel: UserPreferencesViewModel by viewModels<UserPreferencesViewModel>()

    private var _binding: FragmentUserPreferencesBinding? = null

    private val binding get() = _binding!!

    private lateinit var _profilePicture: ImageView
    private lateinit var _userName: TextView
    private lateinit var _userEmail: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uploadProfilePicBtn = binding.uploadThumbnailBtn
        val backBtn = binding.goBackBtn
        val removeAccountBtn = binding.removeAccountBtn

        _profilePicture = binding.profilePictureView
        _userName = binding.username
        _userEmail = binding.email

        updateUserPreferences()

        viewLifecycleOwner.lifecycleScope.launch {
            userPreferenceViewModel.accountDetails.collectLatest { profile ->

                profile.error?.let {
                    Toast.makeText(context, profile.error, Toast.LENGTH_SHORT).show()
                } ?: run {
                    _userEmail.text = profile.userEmail
                    _userName.text = profile.userName
                }
            }
        }

        uploadProfilePicBtn.setOnClickListener {
            profilePicMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        removeAccountBtn.setOnClickListener {

            deleteUserProfileDialog()
        }

        userPreferenceViewModel.accountDeletionRes.observe(viewLifecycleOwner) { deletionRes ->
            if (deletionRes.error == null) {
                val intent = Intent(context, LaunchActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
                Toast.makeText(context, deletionRes.error, Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(context, deletionRes.error, Toast.LENGTH_SHORT).show()
            }
        }

        userPreferenceViewModel.profilePicUploadResult.observe(viewLifecycleOwner) { upload ->

            upload.errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }

            upload.resultMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        userPreferenceViewModel.profilePicLoadResult.observe(viewLifecycleOwner) { update ->

            update.errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }



            Glide.with(_profilePicture)
                .load(update.resultUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(_profilePicture)
        }

    }

    /**
     * Launches dialog informing user about account deletion.
     */
    private fun deleteUserProfileDialog() {
        val dialog = AlertDialog.Builder(this.context);

        dialog.setTitle(R.string.alert_message_title_0);
        dialog.setMessage(R.string.user_account_delete_alert_message);

        dialog.setPositiveButton("Yes") { dialog, which ->
            userPreferenceViewModel.deleteUserProfile()
            dialog.dismiss();
        }

        // move to resources
        dialog.setNegativeButton("No") { dialog, which -> dialog.dismiss(); }

        dialog.show()
    }


    private fun updateUserPreferences() {
        userPreferenceViewModel.updateProfilePicture()
    }

}