package fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import au.edu.uow.journeytracker.app.MainActivity;
import au.edu.uow.journeytracker.app.R;

import dao.DaoSession;
import entity.Coordinate;
import entity.Journey;
import utility.GreenDAOHelper;
import utility.ImageConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;


public class ListJourneysFragment extends Fragment {

    private DaoSession daoSession;

    private List<Journey> journeys;
    private CustomListAdapter adapter = null;

    private OnFragmentInteractionListener mListener;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.daoSession = ((MainActivity) getActivity()).getDaoSession();
        //Fetch data from DB
        this.journeys = ((MainActivity) getActivity()).getDaoSession().getJourneyDao().queryBuilder().list();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journey, container, false);

        //UI Hookup
        final ListView journeyListView = (ListView) view.findViewById(R.id.journeyListView);

        adapter = new CustomListAdapter(getActivity(), (ArrayList) journeys);
        journeyListView.setAdapter(adapter);

        //Set listener
        journeyListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //new ConfirmDialogFragment().show(getActivity().getFragmentManager(), "dialog");
                mListener.startViewJourneyActivity(position);
            }
        });
        journeyListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        // Capture ListView item click
        journeyListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id, boolean checked) {
                // Capture total checked items
                final int checkedCount = journeyListView.getCheckedItemCount();
                // Set the CAB title according to total checked items
                mode.setTitle(checkedCount + " Selected");
                // Calls toggleSelection method from ListViewAdapter Class
                adapter.toggleSelection(position);
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete:
                        // Calls getSelectedIds method from ListViewAdapter Class
                        SparseBooleanArray selected = adapter
                                .getSelectedIds();
                        // Captures all selected ids with a loop
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                Journey selecteditem = (Journey) adapter.getItem(selected.keyAt(i));
                                journeys.remove(selecteditem);
                                new GreenDAOHelper(daoSession).deleteJourney(selecteditem);
                                adapter.notifyDataSetChanged();
                            }
                        }
                        // Close CAB
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.delete_menu, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // TODO Auto-generated method stub
                //listviewadapter.removeSelection();
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // TODO Auto-generated method stub
                return false;
            }
        });

        return view;
    }

    //Custom adapter for the ListView
    public class CustomListAdapter extends BaseAdapter {
        private ArrayList<Journey> listData;

        private LayoutInflater layoutInflater;
        private SparseBooleanArray mSelectedItemsIds;

        public CustomListAdapter(Context context, ArrayList listData) {
            this.listData = listData;
            this.layoutInflater = LayoutInflater.from(context);
            this.mSelectedItemsIds = new SparseBooleanArray();
        }

        @Override
        public int getCount() {
            return listData.size();
        }

        @Override
        public Object getItem(int position) {
            return listData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void toggleSelection(int position) {
            selectView(position, !mSelectedItemsIds.get(position));
        }

        public void selectView(int position, boolean value) {
            if (value)
                mSelectedItemsIds.put(position, value);
            else
                mSelectedItemsIds.delete(position);
            notifyDataSetChanged();
        }

        public int getSelectedCount() {
            return mSelectedItemsIds.size();
        }

        public SparseBooleanArray getSelectedIds() {
            return mSelectedItemsIds;
        }



        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.list_row_layout_journeys, null);
                holder = new ViewHolder();
                holder.nameView = (TextView) convertView.findViewById(R.id.name);
                holder.idView = (TextView) convertView.findViewById(R.id.id);
                holder.distanceView = (TextView) convertView.findViewById(R.id.distance);
                holder.dateView = (TextView) convertView.findViewById(R.id.date);
                holder.imageView = (ImageView) convertView.findViewById(R.id.image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.nameView.setText(" - NAME: " + listData.get(position).getName());
            holder.idView.setText("ID: " + listData.get(position).getId());

            Double distance = 0.0;
            if(listData.get(position).getDistance() != null) distance = round(listData.get(position).getDistance(), 2);
            holder.distanceView.setText("Distance: " + distance);

            String date = " - Date: N/A";
            if(listData.get((position)).getCreatedAt() != null) date = " Date: " + listData.get((position)).getCreatedAt();
            holder.dateView.setText(date);


            boolean foundImage = false;
            for(Coordinate c: listData.get(position).getCoordinates()){
                if(!c.getPhotos().isEmpty()){
                    holder.imageView.setImageBitmap(ImageConverter.convertArrayToImage(c.getPhotos().get(0).getImage()));
                    foundImage = true;
                 }
            }
            if(!foundImage) holder.imageView.setImageResource(R.drawable.photo_placeholder);

            return convertView;
        }

        public double round(double value, int places) {
            if (places < 0) throw new IllegalArgumentException();

            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }

        class ViewHolder {
            TextView nameView;
            TextView idView;
            TextView distanceView;
            TextView dateView;
            ImageView imageView;
        }
    }

    public void notifyDataChange(){
        journeys.clear();
        journeys.addAll(((MainActivity) getActivity()).getDaoSession().getJourneyDao().queryBuilder().list());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        public void startViewJourneyActivity(int position);
    }

}
