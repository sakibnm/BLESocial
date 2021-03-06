package com.example.ifestexplore.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.ifestexplore.Home;
import com.example.ifestexplore.models.Ad;
import com.example.ifestexplore.controllers.AdAdapter;
import com.example.ifestexplore.R;

import java.lang.reflect.Array;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReceivedPosts.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReceivedPosts#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReceivedPosts extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static OnFragmentInteractionListener mListener;

    private static final String TAG = "demo";
    static AdAdapter adAdapter;
    private static ArrayList<Ad> adArrayList = new ArrayList<>();
    private static ArrayList<Ad> favAdArrayList = new ArrayList<>();
    private RecyclerView rv_Ads;
    View view;
    static SwipeRefreshLayout swipeRefreshLayout;

    public ReceivedPosts() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReceivedPosts.
     */
    // TODO: Rename and change types and number of parameters
    public static ReceivedPosts newInstance(String param1, String param2) {
        ReceivedPosts fragment = new ReceivedPosts();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);

            getUpdatedList();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Home.navigationView.getMenu().getItem(0).setChecked(true);
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_received_posts, container, false);
        swipeRefreshLayout = view.findViewById(R.id.received_post_swip);
        swipeRefreshLayout.setOnRefreshListener(ReceivedPosts.this);
        rv_Ads = view.findViewById(R.id.rv_received_posts);
        rv_Ads.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.getActivity());
        rv_Ads.setLayoutManager(linearLayoutManager);
        adAdapter = new AdAdapter(adArrayList, favAdArrayList, getContext(), new AdAdapter.MyClickListener() {

            @Override
            public void onFavoriteClicked(int position, View view) {

            }

            @Override
            public void onForwardClicked(int position, View view) {

            }
        });
        rv_Ads.setAdapter(adAdapter);


//        ___________________________________________________________________________________________
//Fetching others' posts...
        if(mListener!=null)adArrayList = mListener.getOtherAdsArrayList();
        if (mListener!=null)favAdArrayList = mListener.getFavAdsArrayList();
        Log.d(TAG, "Fetched From Fragment: "+adArrayList.toString());
        adAdapter.setAdArrayList(adArrayList);
        adAdapter.notifyDataSetChanged();

//        ___________________________________________________________________________________________



        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRefresh() {
        getUpdatedList();
    }

    public static void getUpdatedList() {
        if(mListener!=null)adArrayList = mListener.getOtherAdsArrayList();
        if(mListener!=null)favAdArrayList = mListener.getFavAdsArrayList();
        adAdapter.setAdArrayList(adArrayList);
        adAdapter.setFavAdArrayList(favAdArrayList);
        Log.d(TAG, "IN RECYCLER VIEW LIST: "+adArrayList.size()+" "+adArrayList);
        adAdapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
    }



    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
        ArrayList<Ad> getOtherAdsArrayList();
        ArrayList<Ad> getFavAdsArrayList();

//        void refreshList();
    }
}
