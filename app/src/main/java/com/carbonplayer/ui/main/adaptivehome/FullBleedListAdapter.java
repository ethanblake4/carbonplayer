package com.carbonplayer.ui.main.adaptivehome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.carbonplayer.R;
import com.carbonplayer.model.entity.proto.innerjam.elements.TitleSectionV1Proto;
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto.*;
import com.carbonplayer.ui.widget.AspectView;
import com.carbonplayer.ui.widget.ParallaxFadeScrimageView;
import com.carbonplayer.ui.widget.ParallaxScrimageView;
import com.carbonplayer.ui.widget.ParallaxScrimageViewSz;
import com.carbonplayer.utils.ColorUtils;
import com.carbonplayer.utils.IdentityUtils;
import com.carbonplayer.utils.MathUtils;
import com.carbonplayer.utils.ProtoUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import timber.log.Timber;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

public final class FullBleedListAdapter extends RecyclerView.Adapter<FullBleedListAdapter.ViewHolder> {

    Context context;

    public FullBleedListAdapter(Context context, List<FullBleedModule> dataset, Function1<FullBleedModule, Unit> callback,
                                RequestManager requestManager) {
        this.context = context;
        this.dataset = dataset;
        this.callback = callback;
        this.requestManager = requestManager;
    }

    private List<FullBleedModule> dataset;
    private Function1<FullBleedModule, Unit> callback;
    private RequestManager requestManager;

    class ViewHolder extends RecyclerView.ViewHolder {

        private FullBleedModule module;

        @BindView(R.id.cardTitle) TextView cardTitle;
        @BindView(R.id.full_bleed_card_img) ParallaxScrimageViewSz image;
        @BindView(R.id.full_bleed_card_grad) View gradient;
        @BindView(R.id.itemText) TextView itemText;
        @BindView(R.id.playButtonSpinner) ProgressBar playButtonSpinner;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
            v.setOnClickListener( view -> callback.invoke(module) );
        }

        void init () {

            /*Timber.d("title: %s\nbackground: %s\n,image-representative: %s\n" +
                    "text: %s\npagination: %s",
                    module.getModuleTitle().getText(),
                    module.getBackgroundColor().getRgbaSpace().toString(),
                    module.getBackgroundImageReference().getRepresentativeColor().getRgbaSpace().toString(),
                    module.getModuleTitle().getColor().getRgbaSpace().toString(),
                    module.getModulePaginationActiveColor().getRgbaSpace().getAlpha());*/

            if(!module.hasModuleSubtitle() || (module.hasModuleSubtitle() &&
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
                cardTitle.setTextColor(ProtoUtils.colorFrom(module.getModuleTitle().getColor()));
                cardTitle.setText(title);

            }

            SpannableString itemTitle = new SpannableString("");

            switch(module.getSingleSection().getContentCase()) {
                case SQUAREPLAYABLECARDLIST:
                    TitleSectionV1Proto.TitleSection ts = module.getSingleSection()
                            .getSquarePlayableCardList().getCards(0).getTitleSection();

                    itemTitle = new SpannableString(ts.getTitle().getText()+"\n"+
                    ts.getSubtitle().getText());
                    break;
                case WIDEPLAYABLECARDLIST:
                    itemTitle = new SpannableString(module.getSingleSection()
                            .getWidePlayableCardList().getCards(0).getTitle().getText());
                    break;
                case TALLPLAYABLECARDLIST:
                    TitleSectionV1Proto.TitleSection ts2 = module.getSingleSection()
                            .getTallPlayableCardList().getCards(0).getTitleSection();

                    itemTitle = new SpannableString(ts2.getTitle().getText()+"\n"+
                            ts2.getSubtitle().getText());
            }

            itemText.setText(itemTitle);

            //gradient.setAspect(module.getBackgroundImageReference().getAspectRatio());

            gradient.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
                    ColorUtils.modifyAlpha(ProtoUtils.colorFrom(
                        module.getBackgroundColor()), 0.1f),
                    ColorUtils.modifyAlpha(ProtoUtils.colorFrom(
                        module.getBackgroundImageReference().getRepresentativeColor()), 1.0f)
            }));

            image.setBackgroundColor(ProtoUtils.colorFrom(module.getBackgroundImageReference().getRepresentativeColor()));

            //image.setAspectRatio(module.getBackgroundImageReference().getAspectRatio());
            //image.setScrimColor(ProtoUtils.colorFrom(module.getBackgroundColor()));
            //image.setScrimColor2(ProtoUtils.colorFrom(module.getBackgroundImageReference().getRepresentativeColor()));
            //image.setScrimAlpha(0.3f);


            requestManager.load(module.getBackgroundImageReference().getUrl())
                    .apply(new RequestOptions().override(
                            Math.round( MathUtils.dpToPx(context, 300) *
                                    MathUtils.aspectWidthMultiple(module.getBackgroundImageReference().getAspectRatio())),
                            MathUtils.dpToPx(context, 300)))
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(image);

        }
    }

    @Override
    public void onViewRecycled(ViewHolder v){

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
        return vh;
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
