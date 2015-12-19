package io.github.voidc.np4ilr.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.voidc.np4ilr.InternetUtils;
import io.github.voidc.np4ilr.R;
import io.github.voidc.np4ilr.model.ILRChannel;

/**
 * A list fragment representing a list of Channels. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ChannelDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ChannelListFragment extends ListFragment {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    public class ChannelAdapter extends ArrayAdapter<ILRChannel> {
        public ChannelAdapter(List<ILRChannel> list) {
            super(getActivity(), R.layout.listitem_channel, list);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ILRChannel channel = getItem(position);

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.listitem_channel, parent, false);
            }

            TextView textName = (TextView) view.findViewById(R.id.textview_listitem_channel_name);
            textName.setText(channel.getFullName());

            TextView textDesc = (TextView) view.findViewById(R.id.textview_listitem_channel_desc);
            textDesc.setText(channel.getDescription());

            //view.setBackgroundColor(InternetUtils.getChannelColor(channel.getId()));

            return view;
        }
    }

    private List<ILRChannel> channelList = new ArrayList<ILRChannel>();

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onChannelSelected(ILRChannel ilrChannel);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onChannelSelected(ILRChannel ilrChannel) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChannelListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ChannelAdapter(channelList));
        fetchChannels();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onChannelSelected(channelList.get(position));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    private void fetchChannels() {
        AsyncTask<Object, Integer, List<ILRChannel>> fetchChannelsTask = new AsyncTask<Object, Integer, List<ILRChannel>>() {
            @Override
            protected List<ILRChannel> doInBackground(Object... params) {
                List<ILRChannel> channelList = null;
                try {
                    channelList = InternetUtils.fetchChannels();
                    InternetUtils.fetchChannelColors();
                } catch (Exception e) {
                    cancel(true);
                }
                return channelList;
            }

            @Override
            protected void onCancelled() {
                Snackbar.make(getListView(), R.string.msg_connection_error, Snackbar.LENGTH_LONG).show();
            }

            @Override
            protected void onPostExecute(List<ILRChannel> ilrChannels) {
                ChannelListFragment.this.channelList.addAll(ilrChannels);
                ((BaseAdapter) ChannelListFragment.this.getListAdapter()).notifyDataSetChanged();
            }
        };
        fetchChannelsTask.execute(null, null);
    }
}
