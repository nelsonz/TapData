/**
 */
package mai.whack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author thanhhaipmai@gmail.com (Thanh Hai Mai)
 */
public class FileListAdapter extends BaseAdapter {
  private Activity activity;
  private int image;
  private String[] list1;
  private static LayoutInflater inflater = null;

  public FileListAdapter(Activity a, int image, String[] list1) {
    this.activity = a;
    this.image = image;
    this.list1 = list1;
    FileListAdapter.inflater =
      (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  @Override
  public int getCount() {
    return list1.length;
  }

  @Override
  public Object getItem(int position) {
    return position;
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  public static class ViewHolder {
    public TextView text1;
    public TextView text2;
    public ImageView image;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View vi = convertView;
    ViewHolder holder;
    if (convertView == null) {

      vi = inflater.inflate(R.layout.filelistadapter, null);

      holder = new ViewHolder();
      holder.text1 = (TextView) vi.findViewById(R.id.item1);
      holder.text2 = (TextView) vi.findViewById(R.id.item2);
      holder.image = (ImageView) vi.findViewById(R.id.icon);

      vi.setTag(holder);
    } else {
      holder = (ViewHolder) vi.getTag();
    }

    holder.text1.setText(this.list1[position]);
    holder.image.setImageResource(this.image);

    final int i = position;
    holder.text1.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        openBrowser("http://192.168.1.192/" + list1[i]);
      }
    });

    return vi;
  }

  private void openBrowser(String url) {
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = "http://" + url;
    }
    Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
    v.vibrate(300);

    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    activity.startActivity(browserIntent);
  }
}
