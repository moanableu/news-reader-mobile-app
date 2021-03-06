package com.example.android.newsapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.example.android.newsapp.ApiQueryBuilder.apiQuery;

public class IntroFragment extends Fragment implements
        LoaderManager.LoaderCallbacks <List <Article>>
        , SharedPreferences.OnSharedPreferenceChangeListener {

    private RecyclerView mRecyclerView;
    private ArticleAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView placeholder;
    private Uri mUrl;
    int pageSize;
    private static final int LOADER_ID = 0;
    private String qUrl;
    String orderBy;

    public IntroFragment() {
        // required
    }

    /**
     * Enable fragment display
     *
     * @param bundle
     * @return
     */
    public static IntroFragment newInstance(Bundle bundle) {
        IntroFragment introFragment = new IntroFragment();
        introFragment.setArguments(bundle);
        return introFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.rb_recyclerview, container, false);

        swipeRefreshLayout = rootView.findViewById(R.id.srl);
        swipeRefreshLayout.setRefreshing(true);

        mRecyclerView = rootView.findViewById(R.id.recyclerView);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        placeholder = rootView.findViewById(R.id.placeholder);

        SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences(String.valueOf(R.string.settings_page_size_key), Context.MODE_PRIVATE);
        Log.e("Prefs", "ok");

        updateOnRefresh();

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (networkConnection()) {
            getLoaderManager().initLoader(LOADER_ID, null, this);
        } else {
            swipeRefreshLayout.setRefreshing(false);
            placeholder.setText(R.string.noNetwork);
            placeholder.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    @Override
    public Loader <List <Article>> onCreateLoader(int id, @Nullable Bundle args) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        pageSize = Integer.parseInt(sharedPrefs.getString(getString(R.string.settings_page_size_key),
                getString(R.string.settings_page_size)));

        orderBy = sharedPrefs.getString(getString(R.string.order_by_key),
                getString(R.string.order_by_default));

        String givenQuery = getArguments().getString("url");
        if (givenQuery != null && !givenQuery.isEmpty()) {
            qUrl = givenQuery;
        } else {
            qUrl = apiQuery(null, pageSize);
        }
        return new ArticleLoader(getContext(), qUrl);
    }

    @Override
    public void onLoadFinished(@NonNull Loader <List <Article>> loader, List <Article> data) {
        // prevent swipe refresh from taking place
        swipeRefreshLayout.setRefreshing(false);

        // hide error message is data available
        placeholder.setVisibility(View.GONE);

        if (data != null && !data.isEmpty()) {

            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mAdapter = new ArticleAdapter((ArrayList <Article>) data);
            mRecyclerView.setAdapter(mAdapter);

            mAdapter.setOnItemClickListener(new ArticleAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    mUrl = Uri.parse(mAdapter.getmArticle().get(position).getUrl());
                    Intent i = new Intent(Intent.ACTION_VIEW, mUrl);
                    startActivity(i);
                }
            });
        } else {
            // error message No Articles to display
            placeholder.setText(R.string.noArticles);
            placeholder.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader <List <Article>> loader) {
    }

    /**
     * verify network connection
     *
     * @return
     */
    private boolean networkConnection() {

        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        return ni != null && ni.isConnected();
    }

    /**
     * update loader is there is a network connection
     */
    private void updateLoader() {
        if (networkConnection()) {
            swipeRefreshLayout.setRefreshing(true);
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        } else {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getContext(), R.string.noNetwork, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * implements SwipeRefreshLayout.OnRefreshListener() method
     */
    private void updateOnRefresh() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!getLoaderManager().hasRunningLoaders()) {
                    updateLoader();
                }
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.settings_page_size))) {
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }
}
