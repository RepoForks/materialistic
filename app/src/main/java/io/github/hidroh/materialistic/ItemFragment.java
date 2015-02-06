package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public class ItemFragment extends Fragment {

    private static final String EXTRA_ITEM = ItemFragment.class.getName() + ".EXTRA_ITEM";
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private ItemManager.Item mItem;
    private int mLocalRevision = 0;
    private boolean mIsResumed;

    public static ItemFragment instantiate(Context context, ItemManager.Item item, Bundle args) {
        final ItemFragment fragment = (ItemFragment) Fragment.instantiate(context,
                ItemFragment.class.getName(), args);
        fragment.mItem = item;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_item, container, false);
        mEmptyView = view.findViewById(android.R.id.empty);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }
        });
        mRecyclerView.setHasFixedSize(true);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            final ItemManager.Item savedItem = savedInstanceState.getParcelable(EXTRA_ITEM);
            if (savedItem != null) {
                mItem = savedItem;
            }
        }

        bindKidData(mItem.getKidItems(), savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsResumed = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ITEM, mItem);
    }

    @Override
    public void onPause() {
        mIsResumed = false;
        super.onPause();
    }

    private void bindKidData(final ItemManager.Item[] items, final @Nullable Bundle savedInstanceState) {
        if (items == null || items.length == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        mRecyclerView.setAdapter(new RecyclerView.Adapter<ItemViewHolder>() {
            @Override
            public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ItemViewHolder(getLayoutInflater(savedInstanceState)
                        .inflate(R.layout.activity_sub_item, parent, false));
            }

            @Override
            public void onBindViewHolder(final ItemViewHolder holder, int position) {
                final ItemManager.Item item = items[position];
                if (item.getLocalRevision() < mLocalRevision) {
                    bindKidItem(holder, null);
                    HackerNewsClient.getInstance(getActivity()).getItem(item.getId(),
                            new ItemManager.ResponseListener<ItemManager.Item>() {
                                @Override
                                public void onResponse(ItemManager.Item response) {
                                    if (response == null) {
                                        return;
                                    }

                                    if (!mIsResumed) {
                                        return;
                                    }

                                    item.populate(response);
                                    item.setLocalRevision(mLocalRevision);
                                    bindKidItem(holder, item);
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    // do nothing
                                }
                            });
                } else {
                    bindKidItem(holder, item);
                }
            }

            private void bindKidItem(final ItemViewHolder holder, final ItemManager.Item item) {
                if (item == null) {
                    holder.mPostedTextView.setText(getString(R.string.loading_text));
                    holder.mContentTextView.setText(getString(R.string.loading_text));
                    holder.mCommentButton.setVisibility(View.INVISIBLE);
                } else {
                    holder.mPostedTextView.setText(item.getDisplayedTime(getActivity()));
                    AppUtils.setTextWithLinks(holder.mContentTextView, item.getText());
                    if (item.getKidCount() > 0) {
                        holder.mCommentButton.setText(String.valueOf(item.getKidCount()));
                        holder.mCommentButton.setVisibility(View.VISIBLE);
                        holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                openItem(holder, item);
                            }
                        });
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                openItem(holder, item);
                            }
                        });
                    }
                }
            }

            @Override
            public int getItemCount() {
                return items.length;
            }

            private void openItem(ItemViewHolder holder, ItemManager.Item item) {
                final Intent intent = new Intent(getActivity(), ItemActivity.class);
                intent.putExtra(ItemActivity.EXTRA_ITEM, item);
                intent.putExtra(ItemActivity.EXTRA_ITEM_LEVEL,
                        getArguments().getInt(ItemActivity.EXTRA_ITEM_LEVEL, 0) + 1);
                final ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(getActivity(),
                                holder.itemView, getString(R.string.transition_item_container));
                ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
            }
        });
    }

    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView mPostedTextView;
        private final TextView mContentTextView;
        private final Button mCommentButton;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mContentTextView = (TextView) itemView.findViewById(android.R.id.text1);
            mCommentButton = (Button) itemView.findViewById(R.id.comment);
            mCommentButton.setVisibility(View.INVISIBLE);
        }
    }
}
