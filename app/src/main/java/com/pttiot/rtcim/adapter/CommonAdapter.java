package com.pttiot.rtcim.adapter;

import java.util.List;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
/**
 * @author  guojianyong
 *     on  2014-4-6
 *   万能适配器
 *   CommonAdapter
 * */
public abstract class CommonAdapter<T> extends BaseAdapter {


	protected LayoutInflater mInflater;
	protected Context context;
	protected List<T> mDatas;
	protected final int mLayoutId;
	protected List<Integer> mLayouts;


	public CommonAdapter(Context context,List<T> mdaList,int mLayoutId) {

		this.context=context;
		this.mDatas=mdaList;
		this.mInflater = LayoutInflater.from(context);
		this.mLayoutId=mLayoutId;
		if (this.mLayouts== null)
		{
			this.mLayouts.add(mLayoutId);
		}
	}
	public CommonAdapter(Context context,List<T> mdaList,List<Integer> mLayoutIds)
	{

		this.context=context;
		this.mDatas=mdaList;
		this.mInflater = LayoutInflater.from(context);
		this.mLayouts=mLayoutIds;
		mLayoutId=0;
	}


	/**
	 *   获取类型
	 * */
	@Override
	public int getItemViewType(int position) {
		// TODO Auto-generated method stub

		int type = getType(position,mDatas.get(position));
		return  type;
	}


	/**
	 * 获取类型总数
	 * */
	@Override
	public int getViewTypeCount() {

		int typeCount = getTypeCount();

		return  typeCount;
	}

	@Override
	public int getCount() {
		return mDatas.size();
	}

	@Override
	public T getItem(int position) {
		// TODO Auto-generated method stub
		return mDatas.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub


		int type =  getItemViewType(position);
		//更改传进去的布局即可
		final ViewHolder viewHolder=getViewHolder(position, convertView, parent,type);
		//抽象一个方法出去 方便进行实现  控件的数据绑定
		convert(viewHolder, getItem(position),type);

		if (viewHolder.getConvertview()==null) {

			Log.i("TAG", "-=-=-=-=-=");
		}
		return viewHolder.getConvertview();
	}
	/**
	 * 对控件进行操作
	 * */
	public  abstract void convert(ViewHolder helper,T item,int type);
	/**
	 * 获取行布局的类型 
	 * */
	public abstract  int  getType(int position,T item);
	/**
	 * 获取行布局的类型数量
	 * */
	public abstract  int  getTypeCount();

	private  ViewHolder getViewHolder(int position,View converView,ViewGroup parent,int type)
	{
		return ViewHolder.get(context, converView, parent, mLayouts.get(type), position);
	}
}
