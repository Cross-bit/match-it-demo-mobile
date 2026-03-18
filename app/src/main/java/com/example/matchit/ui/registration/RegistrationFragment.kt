package com.example.matchit.ui.registration

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.matchit.R
import com.example.matchit.databinding.FragmentRegistrationBinding
import com.example.matchit.ui.login.afterTextChanged
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegistrationFragment : Fragment() {

    private val signupViewModel: RegistrationViewModel by viewModels<RegistrationViewModel>()
    private var _binding: FragmentRegistrationBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailEditText = binding.email
        val usernameEditText = binding.username
        val passwordEditText = binding.password

        val signupBtn = binding.registerBtn
        val goLoginBtn = binding.goLoginBtn

        val loadingProgressBar = binding.loading


        signupViewModel.signupFormState.observe(viewLifecycleOwner,
            Observer {
                val loginState = it ?: return@Observer

                signupBtn.isEnabled = loginState.isDataValid

                loginState.usernameError?.let {
                    usernameEditText.error = getString(loginState.usernameError)
                }

                loginState.passwordError?.let {
                    passwordEditText.error = getString(loginState.passwordError)
                }

                loginState.emailError?.let {
                    emailEditText.error = getString(loginState.emailError)
                }

                loadingProgressBar.visibility = View.GONE
            })

        signupViewModel.signupResult.observe(viewLifecycleOwner, // if this would be activity we would use e.g. this@LoginActivity, as lifecycle owner
            Observer {

                val signupResult = it ?: return@Observer

                loadingProgressBar.visibility = View.GONE

                signupResult.error?.let {
                    showSignUpResult(signupResult.error)
                }

                signupResult.success?.let {
                    showSignUpResult(signupResult.success)
                    findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
                }
            })

        usernameEditText.afterTextChanged {
            signupViewModel.signupDataChanged(
                usernameEditText.text.toString(),
                emailEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }

        emailEditText.afterTextChanged {
            signupViewModel.signupDataChanged(
                usernameEditText.text.toString(),
                emailEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }

        passwordEditText.apply {
            afterTextChanged {
                signupViewModel.signupDataChanged(
                    usernameEditText.text.toString(),
                    emailEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }

            // Listening for the keyboard action key (like e.g. enter...)
            passwordEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    signupViewModel.signupWithCredentials( // if triggered we try login
                        usernameEditText.text.toString(),
                        emailEditText.text.toString(),
                        passwordEditText.text.toString()
                    )
                }
                false
            }

        }

        signupBtn.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE

            signupViewModel.signupWithCredentials(
                usernameEditText.text.toString(),
                emailEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }

        goLoginBtn.setOnClickListener {
            findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
        }

        initialiseGoogleSignIn()

    }

    private fun initialiseGoogleSignIn()
    {
        /*binding.googleSignInButton.setOnClickListener()
        {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId()

        }*/

        //GetGoogleIdOption
    }

    private fun showSignUpResult(@StringRes errorString: Int) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}