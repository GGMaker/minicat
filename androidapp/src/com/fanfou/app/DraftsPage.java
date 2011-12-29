package com.fanfou.app;

import java.io.File;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.fanfou.app.adapter.DraftsCursorAdaper;
import com.fanfou.app.api.Draft;
import com.fanfou.app.db.Contents.DraftInfo;
import com.fanfou.app.dialog.ConfirmDialog;
import com.fanfou.app.service.Constants;
import com.fanfou.app.service.QueueService;
import com.fanfou.app.ui.ActionBar;
import com.fanfou.app.ui.ActionBar.AbstractAction;
import com.fanfou.app.util.StringHelper;
import com.fanfou.app.util.Utils;

/**
 * @author mcxiaoke
 * @version 1.0 2011.10.27
 * @version 1.1 2011.10.28
 * @version 1.2 2011.11.11
 * 
 */
public class DraftsPage extends BaseActivity implements OnItemClickListener {
	private ActionBar mBar;
	private ListView mListView;

	private Cursor mCursor;
	private DraftsCursorAdaper mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setLayout();
	}

	private void setLayout() {
		setContentView(R.layout.list_drafts);
		setActionBar();
		setListView();

	}

	private void setActionBar() {
		mBar = (ActionBar) findViewById(R.id.actionbar);
		mBar.setTitle("草稿箱");
		mBar.setRightAction(new SendAllAction());
	}

	private void setListView() {
		mCursor = managedQuery(DraftInfo.CONTENT_URI, DraftInfo.COLUMNS, null,
				null, null);
		if (mCursor.getCount() == 0) {
			mBar.setRightActionEnabled(false);
		}

		mAdapter = new DraftsCursorAdaper(this, mCursor);
		mListView = (ListView) findViewById(R.id.list);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem clear = menu.add(0, MENU_ID_CLEAR, MENU_ID_CLEAR, "清空草稿");
		clear.setIcon(R.drawable.ic_menu_clear);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case MENU_ID_CLEAR:
			onMenuClearClick();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	@Override
	protected int getPageType() {
		return PAGE_DRAFTS;
	}

	private void onMenuClearClick() {
		getContentResolver().delete(DraftInfo.CONTENT_URI, null, null);
		mCursor.requery();
		Utils.notify(this, "草稿箱已清空");
		finish();
	}

	private class SendAllAction extends AbstractAction {

		public SendAllAction() {
			super(R.drawable.ic_sendall);
		}

		@Override
		public void performAction(View view) {
			doSendAll();
		}

	}

	private void startTaskQueueService() {
		QueueService.start(this);
	}

	private void doSendAll() {
		final ConfirmDialog dialog = new ConfirmDialog(this, "发送所有",
				"确定发送所有草稿吗？");
		dialog.setClickListener(new ConfirmDialog.AbstractClickHandler() {

			@Override
			public void onButton1Click() {
				startTaskQueueService();
				onMenuHomeClick();
			}
		});
		dialog.show();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		final Cursor c = (Cursor) parent.getItemAtPosition(position);
		if (c != null) {
			final Draft draft = Draft.parse(c);
			goWritePage(draft);
		}
	}

	private void goWritePage(final Draft draft) {
		if (draft == null) {
			return;
		}

		Intent intent = new Intent(this, WritePage.class);
		intent.putExtra(Constants.EXTRA_TYPE, draft.type);
		intent.putExtra(Constants.EXTRA_TEXT, draft.text);
		intent.putExtra(Constants.EXTRA_ID, draft.id);
		intent.putExtra(Constants.EXTRA_IN_REPLY_TO_ID, draft.replyTo);
		if (!StringHelper.isEmpty(draft.filePath)) {
			intent.putExtra(Constants.EXTRA_DATA, new File(draft.filePath));
		}
		startActivity(intent);
		finish();

	}

}
