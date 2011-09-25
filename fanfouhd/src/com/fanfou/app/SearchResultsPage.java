package com.fanfou.app;

import java.util.ArrayList;
import java.util.List;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

import com.fanfou.app.adapter.StatusArrayAdapter;
import com.fanfou.app.api.ApiException;
import com.fanfou.app.api.Status;
import com.fanfou.app.api.User;
import com.fanfou.app.config.Commons;
import com.fanfou.app.ui.ActionBar;
import com.fanfou.app.ui.UIManager;
import com.fanfou.app.ui.ActionBar.Action;
import com.fanfou.app.ui.widget.EndlessListView;
import com.fanfou.app.ui.widget.EndlessListView.OnRefreshListener;
import com.fanfou.app.util.StringHelper;
import com.fanfou.app.util.Utils;

public class SearchResultsPage extends BaseActivity implements
		OnRefreshListener, Action,OnItemLongClickListener,OnClickListener {

	protected ActionBar mActionBar;
	protected EndlessListView mListView;
	protected ViewGroup mEmptyView;

	protected StatusArrayAdapter mStatusAdapter;

	private List<Status> mStatuses;

	protected String keyword;
	protected String maxId;

	private boolean showListView = false;

	private static final String tag = SearchResultsPage.class.getSimpleName();

	private void log(String message) {
		Log.e(tag, message);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");
		initialize();
		setLayout();
		search();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		search();

	}

	protected void initialize() {
		mStatuses = new ArrayList<Status>();
	}

	private void setLayout() {
		setContentView(R.layout.list);
		setActionBar();
		mEmptyView = (ViewGroup) findViewById(R.id.empty);
		mListView = (EndlessListView) findViewById(R.id.list);
		mListView.setOnItemLongClickListener(this);
		mListView.setOnRefreshListener(this);
	}

	protected void search() {
		parseIntent();
		mStatuses.clear();
		doSearch();
		showProgress();

	}

	protected void parseIntent() {
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			keyword = intent.getStringExtra(SearchManager.QUERY);
			log("parseIntent() keyword=" + keyword);
		}else if(Intent.ACTION_VIEW.equals(intent.getAction())){
			Uri data = intent.getData();
			if (data != null) {
				keyword = data.getLastPathSegment();
				log("parseIntent() keyword=" + keyword);
			}
		}
	}

	private void showProgress() {
		log("showProgress()");
		showListView = false;
		mListView.setVisibility(View.GONE);
		mEmptyView.setVisibility(View.VISIBLE);
	}

	private void showContent() {
		log("showContent()");
		showListView = true;

		mStatusAdapter = new StatusArrayAdapter(this, mStatuses);
		mListView.setAdapter(mStatusAdapter);

		mEmptyView.setVisibility(View.GONE);
		mListView.removeHeader();
		mListView.setVisibility(View.VISIBLE);
	}

	/**
	 * 初始化和设置ActionBar
	 */
	private void setActionBar() {
		mActionBar = (ActionBar) findViewById(R.id.actionbar);
		mActionBar.setTitle("搜索结果");
		mActionBar.setTitleClickListener(this);
		mActionBar.setLeftAction(new ActionBar.BackAction(mContext));
		mActionBar.setRightAction(this);
		mActionBar.setLeftAction(new ActionBar.BackAction(mContext));
	}

	private void doSearch() {
		if (keyword != null) {
			log("doSearch() keyword=" + keyword);
			new SearchTask().execute();
		}

	}

	protected void updateUI(boolean noMore) {
		log("updateUI()");
		mStatusAdapter.updateDataAndUI(mStatuses);
		if(noMore){
			mListView.onNoLoadMore();
		}else{
			mListView.onLoadMoreComplete();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mListView != null) {
			mListView.restorePosition();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mListView != null) {
			mListView.savePosition();
		}
	}

	@Override
	public void onRefresh(EndlessListView view) {
	}

	@Override
	public void onLoadMore(EndlessListView view) {
		doSearch();
	}

	@Override
	public void onItemClick(EndlessListView view, int position) {
		final Status s = (Status) view.getItemAtPosition(position);
		if (s != null) {
			Utils.goStatusPage(mContext, s);
		}
	}

	private class SearchTask extends AsyncTask<Void, Void, List<Status>> {

		@Override
		protected void onPreExecute() {
		}

		protected void onPostExecute(List<com.fanfou.app.api.Status> result) {
			if (!showListView) {
				showContent();
			}
			if (result != null && result.size() > 0) {

				int size = result.size();
				log("result size=" + size);
				maxId = result.get(size - 1).id;
				log("maxId=" + maxId);

				mStatuses.addAll(result);
				updateUI(size<20);
			}
		}

		@Override
		protected List<com.fanfou.app.api.Status> doInBackground(Void... params) {
			if (StringHelper.isEmpty(keyword)) {
				return null;
			}
			List<com.fanfou.app.api.Status> result = null;
			try {
				result = App.me.api.search(keyword, maxId,true);
			} catch (ApiException e) {
				e.printStackTrace();
			}

			return result;
		}

	}

	@Override
	public int getDrawable() {
		return R.drawable.i_write;
	}

	@Override
	public void performAction(View view) {
		Intent intent = new Intent(this, WritePage.class);
		intent.putExtra(Commons.EXTRA_TYPE, WritePage.TYPE_NORMAL);
		startActivity(intent);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		final Status s = (Status) parent.getItemAtPosition(position);
		showPopup(view, s);
		return true;
	}
	
	private void showPopup(final View view, final Status s) {
		if (s == null||s.isNull()) {
			return;
		}
		UIManager.showPopup(this, view, s, mStatusAdapter, mStatuses);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.actionbar_title:
			goTop();
			break;
		default:
			break;
		}
	}
	
	private void goTop(){
		if(mListView!=null){
			mListView.setSelection(0);
		}
	}

}