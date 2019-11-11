package com.pttiot.rtcim.adapter;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author gjy
 * 
 *  通用的ViewHolder
 * 
 * **/
public class ViewHolder {

	//存放绑定的控件的容器
	private final SparseArray<View> mViews;
	//item视图
	private    View mConvertView;

	private  ViewHolder(Context context,ViewGroup parent,int layoutId,int position) 
	{
		this.mViews=new SparseArray<View>();
		//初始化视图
		mConvertView=LayoutInflater.from(context).inflate(layoutId, null,false);
		//绑定Tag
		mConvertView.setTag(this);
	}


	/**
	 *  获取Viewholder的对象  为类的入口
	 *  @param context 上下文
	 *  @param convertview item视图
	 *  @param layoutId item布局的id
	 *  @param position 当前item的position
	 *  
	 * */
	public static ViewHolder get(Context context,View convertview,ViewGroup parent,int layoutId,int position)
	{

		if (convertview==null) {

			Log.i("TAG", "weikong---------------");
			return new ViewHolder(context, parent, layoutId, position);
		}
		 

		return  (ViewHolder) convertview.getTag();

	}
	/**
	 *  通过控件的id获取对于控件，如果没有加入view
	 * */
	@SuppressWarnings("unchecked")
	public <T extends View>T getView(int viewId)
	{
		View view=mViews.get(viewId);
		if (view == null) {

			view=mConvertView.findViewById(viewId);
			mViews.put(viewId, view);

		}

		return (T)view;
	}
	/***
	 * 为TextView 设置字符串
	 * **/
	public ViewHolder setText(int viewId,String text)
	{
		TextView view=getView(viewId);
		view.setText(text);
		return this;
	}
	/***
	 * 为ImageView 设置图片资源
	 * **/
	public ViewHolder setImageResource(int viewId,int drawableId)
	{
		ImageView img=getView(viewId);
		img.setImageResource(drawableId);
		return this;
	}
	/***
	 * 为ImageView 通过URL访问图片
	 * **/
	public ViewHolder setImageResource(int viewId,String url)
	{
		ImageView img=getView(viewId);
		//UILUtils.displayImageNoAnim(url, img);
		return this;
	}
	/***
	 * 为Button 设置text
	 * **/
	public ViewHolder setButtonText(int viewId,String txt)
	{
		Button btn=getView(viewId);
		btn.setText(txt);
		return this;
	}


	public View getConvertview()
	{
		return mConvertView;
	}
}
