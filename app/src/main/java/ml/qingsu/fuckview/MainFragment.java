package ml.qingsu.fuckview;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import ml.qingsu.fuckview.wizard.TutorialWizard;

/**
 * By w568w on 2017-7-6.
 */

public class MainFragment extends Fragment implements Searchable {
    private AppAdapter adapter;
    private Activity context;
    PackageManager pm;
    ArrayList<MainActivity.BlockModel> models;
    ListView listView;
    String searchText = "";
    private ArrayList<Integer> deleteList = new ArrayList<>();

    public static String getAppTitle(PackageManager pm, String packageName) {
        try {
            return pm.getPackageInfo(packageName, 0).applicationInfo.loadLabel(pm).toString();

        } catch (Exception ignored) {
        }
        return packageName;
    }

    public static Drawable getAppIcon(PackageManager pm, String packageName) {
        try {
            Drawable drawable = pm.getPackageInfo(packageName, 0).applicationInfo.loadIcon(pm);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            return drawable;

        } catch (Exception ignored) {
        }
        return new BitmapDrawable();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getActivity();
        pm = context.getPackageManager();
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.main_fragment, null);
        listView = (ListView) layout.findViewById(R.id.listView);
        models = MainActivity.read();
        Collections.sort(models, new Comparator<MainActivity.BlockModel>() {
            @Override
            public int compare(MainActivity.BlockModel blockModel, MainActivity.BlockModel t1) {
                String s1 = getAppTitle(pm, blockModel.packageName);
                String s2 = getAppTitle(pm, t1.packageName);
                return Collator.getInstance(Locale.CHINA).compare(s1, s2);
            }
        });
        adapter = new AppAdapter();
        listView.setAdapter(adapter);
        registerForContextMenu(listView);
        final FloatingActionButton button = (FloatingActionButton) layout.findViewById(R.id.fab);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TutorialWizard tw = new TutorialWizard();
                Bundle bundle = new Bundle();
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("system_app", false))
                    bundle.putBoolean("sys", true);
                tw.setArguments(bundle);
                if (context instanceof MainActivity)
                    ((MainActivity) context).setFragment(tw);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //是否是强制屏蔽
                MainActivity.BlockModel model = models.get(i);
                Bundle bundle = new Bundle();
                bundle.putString("pkg", model.packageName);
                bundle.putString("id", model.id);
                bundle.putString("className", model.className);
                InfoFragment infoFragment = new InfoFragment();
                infoFragment.setArguments(bundle);
                if (context instanceof MainActivity)
                    ((MainActivity) context).setFragment(infoFragment);
            }
        });
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (context instanceof MainActivity) {
            ((MainActivity) context).shouldShowFAQ = true;
            ((MainActivity) context).currentFragment = this;
            //AppCompatActivity类提供了一个可以在API9上使用的invalidateOptionsMenu方法
            //本来这个方法在Activity类上需要API11的
            //所以需要转型
            //(PS:呆萌的AS告诉我转型是多余的2333)
            ((AppCompatActivity) context).invalidateOptionsMenu();

        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        MainActivity.BlockModel model = models.get(info.position);
        if (deleteList.size() > 0) {
            menu.add(0, 4, Menu.NONE, "全部删除");
            menu.add(0, 5, Menu.NONE, "全部分享");
            return;
        }
        menu.add(0, 1, Menu.NONE, "删除");
        if (model.id.endsWith("$$") && !model.className.equals("*")) {
            menu.add(0, 2, Menu.NONE, "设为不按类名定位");
        } else {
            menu.add(0, 3, Menu.NONE, "分享规则");
        }
        menu.add(0, 6, Menu.NONE, model.enable?"禁用此项":"启用此项");
    }

    public void setSearchText(String text) {
        searchText = text;
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final MainActivity.BlockModel model = models.get(menuInfo.position);
        switch (item.getItemId()) {
            case 1:
                models.remove(menuInfo.position);
                adapter.notifyDataSetChanged();
                saveAll();
                break;
            case 2:
                new AlertDialog.Builder(context)
                        .setTitle("确认")
                        .setMessage("是否要将此项设为不按类名定位？\n这样可能会解决一些无法屏蔽的问题，但是也会增加一些误伤的几率。\n\n注意:此操作不可逆!")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                model.className = "*";
                                models.set(menuInfo.position, model);
                                adapter.notifyDataSetChanged();
                                saveAll();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                break;
            case 3:
                share(model.toString());
                break;
            case 4:
                ArrayList<MainActivity.BlockModel> arrayList = new ArrayList<>();
                for (Integer postion : deleteList)
                    arrayList.add(models.get(postion));
                models.removeAll(arrayList);
                deleteList.clear();
                adapter.notifyDataSetChanged();

                saveAll();
                break;
            case 5:
                ArrayList<MainActivity.BlockModel> shares = new ArrayList<>();
                for (Integer postion : deleteList)
                    shares.add(models.get(postion));
                deleteList.clear();
                StringBuilder stringBuilder = new StringBuilder();
                for (MainActivity.BlockModel model1 : shares) {
                    stringBuilder.append(model1.toString());
                    stringBuilder.append("\n");
                }
                share(stringBuilder.toString());
                adapter.notifyDataSetChanged();
                break;
            case 6:
                model.enable=!model.enable;
                models.set(menuInfo.position, model);
                adapter.notifyDataSetChanged();
                saveAll();
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void share(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType("text/plain");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "在您的设备上未发现分享应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAll() {
        MainActivity.Write_File("", MainActivity.LIST_NAME);
        for (MainActivity.BlockModel bm : models)
            bm.save();
    }

    private class AppAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return models.size();
        }

        @Override
        public Object getItem(int i) {
            return models.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private boolean contains(ArrayList<Integer> arrayList, int i) {
            for (Integer integer : arrayList) {
                if (integer == i)
                    return true;
            }
            return false;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            if (view == null || !(view instanceof ViewGroup))
                view = context.getLayoutInflater().inflate(R.layout.main_fragment_list_item, null);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            TextView title = (TextView) view.findViewById(R.id.app_name);
            TextView type = (TextView) view.findViewById(R.id.class_name);
            final CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
            checkbox.setChecked(contains(deleteList, i));
            checkbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isChecked = contains(deleteList, i);
                    if (!isChecked) {
                        deleteList.add(i);
                    } else {
                        deleteList.remove(Integer.valueOf(i));
                    }
                }
            });
            MainActivity.BlockModel bm = models.get(i);
            try {
                icon.setImageDrawable(getAppIcon(pm, bm.packageName));
                title.setText(getAppTitle(pm, bm.packageName));
            } catch (Exception e) {
                icon.setImageResource(R.drawable.ic_launcher);
                title.setText(bm.packageName);
            }
            if (bm.text.equals(""))
                type.setText(bm.className);
            else
                type.setText(String.format(Locale.CHINA, "%s ---> %s", bm.className, bm.text));
            if(!bm.enable)
                view.setBackgroundColor(Color.GRAY);
            if (!searchText.equals("") && !title.getText().toString().toLowerCase().contains(searchText.toLowerCase())) {
                view = new View(context);
                view.setVisibility(View.GONE);
            }
            return view;
        }

    }
}
