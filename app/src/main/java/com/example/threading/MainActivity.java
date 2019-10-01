package com.example.threading;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.threading.adapters.WordsRecyclerAdapter;
import com.example.threading.models.Word;
import com.example.threading.threading.MyThread;
import com.example.threading.util.Constants;
import com.example.threading.util.VerticalSpacingItemDecorator;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements WordsRecyclerAdapter.OnWordListener, View.OnClickListener, SwipeRefreshLayout.OnRefreshListener, Handler.Callback{
    private static final String TAG = "DictionaryActivity";

    //ui components
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefresh;

    //vars
    private ArrayList<Word> mWords = new ArrayList<>();
    private WordsRecyclerAdapter mWordRecyclerAdapter;
    private FloatingActionButton mFab;
    private String mSearchQuery = "";
    private MyThread mMyThread;
    private Handler mMainThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recyclerView);
        mFab = findViewById(R.id.fab);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);

        mFab.setOnClickListener(this);
        mSwipeRefresh.setOnRefreshListener(this);

        mMainThreadHandler = new Handler(this);

        setupRecyclerView();

    }

    private void restoreInstanceState(Bundle savedInstanceState){
        if(savedInstanceState != null){
            mWords = savedInstanceState.getParcelableArrayList("words");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("words", mWords);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: called.");
        super.onStart();
        mMyThread = new MyThread(this, mMainThreadHandler);
        mMyThread.start();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called.");
        super.onStop();
        mMyThread.quitThread();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mSearchQuery.length() > 2){
            onRefresh();
        }
    }

    private void retrieveWords(String title) {
        Log.d(TAG, "retrieveWords: called.");
        Message message = Message.obtain(null, Constants.WORDS_RETRIEVE);
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        message.setData(bundle);
        mMyThread.sendMessageToBackgroundThread(message);
    }


    public void deleteWord(Word word) {
        Log.d(TAG, "deleteWord: called.");
        mWords.remove(word);
        mWordRecyclerAdapter.getFilteredWords().remove(word);
        mWordRecyclerAdapter.notifyDataSetChanged();

        Message message = Message.obtain(null, Constants.WORD_DELETE);
        Bundle bundle = new Bundle();
        bundle.putParcelable("word_delete", word);
        message.setData(bundle);
        mMyThread.sendMessageToBackgroundThread(message);
    }



    private void setupRecyclerView(){
        Log.d(TAG, "setupRecyclerView: called.");
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        VerticalSpacingItemDecorator itemDecorator = new VerticalSpacingItemDecorator(10);
        mRecyclerView.addItemDecoration(itemDecorator);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);
        mWordRecyclerAdapter = new WordsRecyclerAdapter(mWords, this);
        mRecyclerView.setAdapter(mWordRecyclerAdapter);
    }

    @Override
    public void onWordClick(int position) {
        Intent intent = new Intent(this, EditWordActivity.class);
        intent.putExtra("selected_word", mWords.get(position));
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.fab:{
                Intent intent = new Intent(this, EditWordActivity.class);
                startActivity(intent);
                break;
            }

        }
    }


    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            deleteWord(mWords.get(mWords.indexOf(mWordRecyclerAdapter.getFilteredWords().get(viewHolder.getAdapterPosition()))));
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView =
                (SearchView) searchItem.getActionView();

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                if(query.length() > 2){
                    mSearchQuery = query;
                    retrieveWords(mSearchQuery);
                }
                else{
                    clearWords();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                if(query.length() > 2){
                    mSearchQuery = query;
                    retrieveWords(mSearchQuery);
                }
                else{
                    clearWords();
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void clearWords(){
        if(mWords != null){
            if(mWords.size() > 0){
                mWords.clear();
            }
        }
        mWordRecyclerAdapter.getFilter().filter(mSearchQuery);
    }

    @Override
    public void onRefresh() {
        retrieveWords(mSearchQuery);
        mSwipeRefresh.setRefreshing(false);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){

            case Constants.WORDS_RETRIEVE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully retrieved words. This is from thread: " + Thread.currentThread().getName());

                clearWords();

                ArrayList<Word> words = new ArrayList<>(msg.getData().<Word>getParcelableArrayList("words_retrieve"));
                mWords.addAll(words);
                mWordRecyclerAdapter.getFilter().filter(mSearchQuery);
                break;
            }

            case Constants.WORDS_RETRIEVE_FAIL:{
                Log.d(TAG, "handleMessage: unable to retrieve words. This is from thread: " + Thread.currentThread().getName());

                clearWords();
                break;
            }

            case Constants.WORD_INSERT_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully inserted new word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_INSERT_FAIL:{
                Log.d(TAG, "handleMessage: unable to insert new word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_DELETE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully deleted a word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_DELETE_FAIL:{
                Log.d(TAG, "handleMessage: unable to delete word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

        }
        return true;
    }
}
