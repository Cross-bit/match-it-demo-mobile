package com.example.matchit.ui.login

import android.content.Intent
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
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.matchit.ui.MainActivity
import com.example.matchit.R
import com.example.matchit.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val loginViewModel: LoginViewModel by viewModels<LoginViewModel>()
    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameEditText = binding.username
        val passwordEditText = binding.password

        val loginBtn = binding.loginBtn
        val registerBtn = binding.signUpBtn
        val loadingProgressBar = binding.loading
        //val forgotPasswordBtn = binding.forgotPasswordBtn

        // Every time we are validating the text
        loginViewModel.loginFormState.observe(viewLifecycleOwner,
            Observer {

                val loginState = it ?: return@Observer

                loginBtn.isEnabled = loginState.isDataValid

                loginState.usernameError?.let {
                    usernameEditText.error = getString(loginState.usernameError)
                }

                loginState.passwordError?.let {
                    passwordEditText.error = getString(loginState.passwordError)
                }

            })

        // After the login is submitted and response is recieved
        loginViewModel.loginResult.observe(viewLifecycleOwner, // if this would be activity we would use e.g. this@LoginActivity, as lifecycle owner
            Observer {

                val loginResult = it ?: return@Observer

                loadingProgressBar.visibility = View.GONE

                loginResult.error?.let {
                    showLoginFailed(loginResult.error)
                }
                loginResult.success?.let {
                    updateUiWithUser(loginResult.success)
                }
            })



        usernameEditText.afterTextChanged {
            loginViewModel.loginDataChanged(
                usernameEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }

        passwordEditText.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }

            // Listening for the keyboard action key (like e.g. enter...)
            passwordEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login( // if triggered we try login
                        usernameEditText.text.toString(),
                        passwordEditText.text.toString()
                    )
                }
                false
            }
        }

        loginBtn.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            loginViewModel.login(
                usernameEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }

        registerBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registrationFragment)
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcomeMessage = String.format(getString(R.string.welcome), model.displayName)

        val appContext = context?.applicationContext ?: return

        Toast.makeText(
            appContext,
            welcomeMessage,
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(appContext, MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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