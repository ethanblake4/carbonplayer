package com.carbonplayer.ui.main.adaptivehome;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.carbonplayer.R;
import com.carbonplayer.model.entity.proto.innerjam.elements.TitleSectionV1Proto;
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto.FullBleedModule;
import com.carbonplayer.model.entity.proto.innerjam.visuals.AttributedTextV1Proto;
import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto;
import com.carbonplayer.ui.widget.ParallaxScrimageViewSz;
import com.carbonplayer.utils.protocol.ProtoUtils;
import com.carbonplayer.utils.ui.ColorUtils;

import java.util.LinkedList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Action;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

public final class FullBleedListAdapter
        extends RecyclerView.Adapter<FullBleedListAdapter.ViewHolder> {

    RecyclerView recycler;

    public FullBleedListAdapter(List<FullBleedModule> dataset,
                                Function1<FullBleedModule, Unit> callback,
                                Function2<FullBleedModule, Action, Unit> playCallback,
                                RequestManager requestManager, RecyclerView my) {
        //this.context = context;
        this.dataset = dataset;
        this.callback = callback;
        this.playCallback = playCallback;
        this.requestManager = requestManager;
        recycler = my;
    }

    private List<FullBleedModule> dataset;
    private Function1<FullBleedModule, Unit> callback;
    private Function2<FullBleedModule, Action, Unit> playCallback;
    private RequestManager requestManager;
    private List<Action> scrollCallbacks = new LinkedList<>();

    class ViewHolder extends RecyclerView.ViewHolder {

        private FullBleedModule module;

        RecyclerView.OnScrollListener listener;

        @BindView(R.id.cardTitle) TextView cardTitle;
        @BindView(R.id.full_bleed_card_img) ParallaxScrimageViewSz image;
        @BindView(R.id.full_bleed_card) FrameLayout card;
        @BindView(R.id.full_bleed_card_grad) View gradient;
        @BindView(R.id.itemText) TextView itemText;
        @BindView(R.id.playButtonSpinner) ProgressBar playButtonSpinner;
        @BindView(R.id.playButton) ImageButton playButton;
        @BindView(R.id.moduleTitleUnderline) View underline;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
            v.setOnClickListener(view -> callback.invoke(module));
            playButton.setOnClickListener(view -> {
                playButtonSpinner.setVisibility(View.VISIBLE);
                playCallback.invoke(module,
                        () -> playButtonSpinner.setVisibility(View.GONE));
            });
            v.setLayoutParams(v.getLayoutParams());
        }

        void init() {

            if (!module.hasModuleSubtitle() || (module.hasModuleSubtitle() &&
                    module.getModuleSubtitle().getText().length() == 0))
                cardTitle.setText(module.getModuleTitle().getText());
            else {
                String titleText = module.getModuleTitle().getText();
                String subText = module.getModuleSubtitle().getText();
                SpannableString title = new SpannableString(titleText + "\n\n" + subText);
                title.setSpan(new StyleSpan(Typeface.ITALIC), titleText.length(),
                        titleText.length() + subText.length() + 2, SPAN_INCLUSIVE_INCLUSIVE);
                title.setSpan(new RelativeSizeSpan(0.5f), titleText.length() + 2,
                        titleText.length() + subText.length() + 2, SPAN_INCLUSIVE_INCLUSIVE);
                cardTitle.setText(title);
            }

            //Timber.d(module.getModuleTitle().getColor().toString());
            cardTitle.setTextColor(ProtoUtils.colorFrom(module.getModuleTitle().getColor()));

            underline.setBackgroundColor(ProtoUtils.colorFrom(
                    module.getModuleTitleUnderlineColor()));

            SpannableString itemTitle = new SpannableString("");

            int itemColor = Color.WHITE;

            switch (module.getSingleSection().getContentCase()) {
                case SQUAREPLAYABLECARDLIST:
                    TitleSectionV1Proto.TitleSection ts = module.getSingleSection()
                            .getSquarePlayableCardList().getCards(0).getTitleSection();

                    itemTitle = new SpannableString(ts.getTitle().getText() + "\n" +
                            ts.getSubtitle().getText());
                    itemColor = ProtoUtils.colorFrom(ts.getTitle().getColor());
                    break;
                case WIDEPLAYABLECARDLIST:
                    AttributedTextV1Proto.AttributedText at = module.getSingleSection()
                            .getWidePlayableCardList().getCards(0).getTitle();
                    itemTitle = new SpannableString(at.getText());
                    itemColor = ProtoUtils.colorFrom(at.getColor());
                    break;
                case TALLPLAYABLECARDLIST:
                    TitleSectionV1Proto.TitleSection ts2 = module.getSingleSection()
                            .getTallPlayableCardList().getCards(0).getTitleSection();
                    itemColor = ProtoUtils.colorFrom(ts2.getTitle().getColor());
                    itemTitle = new SpannableString(ts2.getTitle().getText() + "\n" +
                            ts2.getSubtitle().getText());
            }

            itemText.setText(itemTitle);

            itemText.setTextColor(ProtoUtils.colorFrom(module.getModuleTitle().getColor()));

            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            ColorUtils.modifyAlpha(ProtoUtils.colorFrom(
                                    module.getBackgroundColor()), 0.1f),
                            ColorUtils.modifyAlpha(ProtoUtils.colorFrom(
                                    module.getBackgroundImageReference().getRepresentativeColor()), 1.0f)
                    });

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                image.setForeground(g);
                if(module.getBackgroundImageReference().getAspectRatio() ==
                        ImageReferenceV1Proto.ImageReference.AspectRatio.ONE_BY_ONE) {
                    image.setAdjustViewBounds(false);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            } else {
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                gradient.setBackground(g);
            }



            card.setBackgroundColor(ProtoUtils.colorFrom(
                    module.getBackgroundImageReference().getRepresentativeColor()));

            requestManager.load(module.getBackgroundImageReference().getUrl())
                    .transition(DrawableTransitionOptions.with((dataSource, isFirstResource) -> {
                        if (dataSource == DataSource.RESOURCE_DISK_CACHE
                                || dataSource == DataSource.DATA_DISK_CACHE
                                || dataSource == DataSource.MEMORY_CACHE) return null;
                        return new DrawableCrossFadeFactory.Builder(200).build()
                                .build(dataSource, isFirstResource);
                    }))
                    .into(image);
        }
    }

    @Override
    public void onViewRecycled(ViewHolder vh) {
        //recycler.removeOnScrollListener(vh.listener);
        requestManager.clear(vh.image);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FullBleedListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.full_bleed_module, parent, false);
        // set the view's size, margins, paddings and layout parameters

        ViewHolder vh = new ViewHolder(v);

        vh.listener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //vh.image.setOffset(vh.image.getOffset() + dy);
                int[] loc = new int[2];
                vh.image.getLocationInWindow(loc);
                vh.image.setTranslationY(-loc[1] * 0.12f);
                //vh.gradient.setTranslationY(-loc[1] * 0.12f);
                super.onScrolled(recyclerView, dx, dy);
            }
        };

        recycler.addOnScrollListener(vh.listener);

        scrollCallbacks.add(() -> {
            int[] loc = new int[2];
            vh.image.getLocationInWindow(loc);
            vh.image.setTranslationY(-loc[1] * 0.12f);
        });

        return vh;
    }

    public Action getScrollCallback() {
        return () -> {
            for (Action cb: scrollCallbacks) {
                cb.run();
            }
        };
    }


    // Replace the contents of a view (invoked by the layout manager)
    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.module = dataset.get(position);
        holder.init();
    }

    public int getItemCount() {
        return dataset.size();
    }

}
