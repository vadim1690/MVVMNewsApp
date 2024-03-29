package com.androiddevs.mvvmnewsapp.ui.fragments


import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.androiddevs.mvvmnewsapp.R
import com.androiddevs.mvvmnewsapp.adapters.NewsAdapter
import com.androiddevs.mvvmnewsapp.ui.MainActivity
import com.androiddevs.mvvmnewsapp.ui.NewsViewModel
import com.androiddevs.mvvmnewsapp.util.Constants.Companion.QUERY_PAGE_SIZE
import com.androiddevs.mvvmnewsapp.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BreakingNewsFragment : Fragment(R.layout.fragment_breaking_news) {

    lateinit var viewModel: NewsViewModel
    lateinit var newsAdapter: NewsAdapter
    lateinit var recyclerView: RecyclerView
    lateinit var progressBar: ProgressBar

    var isLoading = false
    var isLastPage = false
    var isScrolling = false

    val scrollListener = object : OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalItemCount >= QUERY_PAGE_SIZE
            val shouldPaginate = isNotLoadingAndNotLastPage &&
                    isAtLastItem &&
                    isNotAtBeginning &&
                    isTotalMoreThanVisible &&
                    isScrolling

            if (shouldPaginate) {
                viewModel.getBreakingNews("us")
                isScrolling = false
            }

        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                // currently scrolling
                isScrolling = true
            }
        }
    }

    val TAG = "BreakingNewsFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = (activity as MainActivity).viewModel
        findViews(view)
        setRecyclerView()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.breakingNewsFlow.collectLatest { response ->
                    when (response) {
                        is Resource.Success -> {
                            hideProgressBar()
                            response.data?.let { newsResponse ->
                                newsAdapter.differ.submitList(newsResponse.articles.toList())
                                val totalPages = newsResponse.totalResults / QUERY_PAGE_SIZE + 2
                                isLastPage = viewModel.breakingNewsPage == totalPages
                                if (isLastPage) {
                                    recyclerView.setPadding(0, 0, 0, 0)
                                }
                            }
                        }
                        is Resource.Error -> {
                            hideProgressBar()
                            response.message?.let { message ->
                                Toast.makeText(
                                    activity,
                                    "An error occurred: $message",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        }
                        is Resource.Loading -> showProgressBar()
                        else -> Unit
                    }
                }
            }
        }

//        viewModel.breakingNews.observe(viewLifecycleOwner) {
//        }

    }

    private fun hideProgressBar() {
        progressBar.visibility = INVISIBLE
        isLoading = false
    }

    private fun showProgressBar() {
        progressBar.visibility = VISIBLE
        isLoading = true
    }

    private fun findViews(view: View) {
        recyclerView = view.findViewById(R.id.rvBreakingNews)
        progressBar = view.findViewById(R.id.paginationProgressBar)
    }

    private fun setRecyclerView() {
        newsAdapter = NewsAdapter()
        newsAdapter.setOnItemClickListener { article ->
            val bundle = Bundle().apply {
                putParcelable("article", article)
            }
            findNavController().navigate(
                R.id.action_breakingNewsFragment_to_articleFragment,
                bundle
            )
        }
        recyclerView.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@BreakingNewsFragment.scrollListener)
        }
    }
}