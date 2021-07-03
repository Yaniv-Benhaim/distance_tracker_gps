package co.il.dronedistancetracker.ui.portfolio

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import co.il.dronedistancetracker.R
import co.il.dronedistancetracker.data.other.Constants.PORTFOLIO_URL
import kotlinx.android.synthetic.main.fragment_portfolio.*

class PortfolioFragment : Fragment(R.layout.fragment_portfolio) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.apply {
            loadUrl(PORTFOLIO_URL)
            settings.javaScriptEnabled = true
        }
    }

}