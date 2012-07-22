/**
 */
package mai.whack;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

/**
 * @author thanhhaipmai@gmail.com (Thanh Hai Mai)
 */
public class FileListActivity extends Activity {
  private int image = R.drawable.icon;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.filelist);

    String[] list1 = getIntent().getExtras().getStringArray("listOfFiles");

    ListView lv = (ListView) findViewById(R.id.lvResult);

    FileListAdapter listAdapter = new FileListAdapter(FileListActivity.this, image, list1);
    lv.setAdapter(listAdapter);
  }
}
