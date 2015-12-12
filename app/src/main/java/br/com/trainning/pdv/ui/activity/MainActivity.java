package br.com.trainning.pdv.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.silverforge.controls.BusyIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import br.com.trainning.pdv.R;
import br.com.trainning.pdv.domain.model.Carrinho;
import br.com.trainning.pdv.domain.model.Item;
import br.com.trainning.pdv.domain.model.ItemProduto;
import br.com.trainning.pdv.domain.model.ListaProdutos;
import br.com.trainning.pdv.domain.model.Produto;
import br.com.trainning.pdv.domain.network.APIClient;
import br.com.trainning.pdv.domain.util.Util;
import br.com.trainning.pdv.ui.adapter.CustomArrayAdapter;
import butterknife.Bind;
import jim.h.common.android.lib.zxing.config.ZXingLibConfig;
import jim.h.common.android.lib.zxing.integrator.IntentIntegrator;
import jim.h.common.android.lib.zxing.integrator.IntentResult;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

public class MainActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.listView)
    SwipeMenuListView listView;
    @Bind(R.id.coodenator1)
    CoordinatorLayout coordinatorLayout;

    @Bind(R.id.busyIndicator)
    BusyIndicator busyIndicator;

    private ZXingLibConfig zxingLibConfig;

    private Carrinho carrinho;
    private Item item;
    private List<ItemProduto> list;
    private CustomArrayAdapter adapter;
    double valorTotal;
    int quantidadeItens=0;

    private Callback<List<Produto>> callbackProdutos;

    private InfiniteBusyModifier infiniteBusyModifier;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(toolbar);

        configureProdutoCallback();

        zxingLibConfig = new ZXingLibConfig();
        zxingLibConfig.useFrontLight = true;


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                IntentIntegrator.initiateScan(MainActivity.this, zxingLibConfig);

            }
        });

        CursorList li = Query.all(Carrinho.class).get();
        List<Carrinho> i = li.asList();
        for(Carrinho it: i){
            Log.d("Carrinho", ""+it.toString());
        }

        carrinho = Query.one(Carrinho.class, "select * from carrinho where id = (select max(id) from carrinho)").get();


        if(carrinho == null){
            criaCarrinho();
        }else{
            if(carrinho.getEncerrada()==1){
                criaCarrinho();
            }
        }
        Log.d("IDCOMPRA",""+carrinho.toString());

        popularLista();



        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                //create an action that will be showed on swiping an item in the list
                SwipeMenuItem item1 = new SwipeMenuItem(
                        getApplicationContext());
                item1.setBackground(R.color.colorAccent);
                item1.setWidth(120);
                item1.setIcon(R.drawable.ic_add_shopping_cart_white_36dp);
                menu.addMenuItem(item1);

                SwipeMenuItem item2 = new SwipeMenuItem(
                        getApplicationContext());

                item2.setBackground(R.color.colorDelete);
                item2.setWidth(120);
                item2.setIcon(R.drawable.ic_delete_white_36dp);
                menu.addMenuItem(item2);
            }
        };
        //set MenuCreator
        listView.setMenuCreator(creator);
        // set SwipeListener
        listView.setOnSwipeListener(new SwipeMenuListView.OnSwipeListener() {

            @Override
            public void onSwipeStart(int position) {
                // swipe start
            }

            @Override
            public void onSwipeEnd(int position) {
                // swipe end
            }
        });

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                ItemProduto itemProduto = adapter.getItem(position);
                Item item = Query.one(Item.class,"select * from item where id = ?", itemProduto.getIdItem()).get();
                switch (index) {
                    case 0:

                        //Toast.makeText(getApplicationContext(), "Action 1 for " + itemProduto.getDescricao(), Toast.LENGTH_SHORT).show();

                        item.setQuantidade(item.getQuantidade()+1);
                        item.save();
                        list.clear();
                        popularLista();
                        break;
                    case 1:
                        //Toast.makeText(getApplicationContext(), "Action 2 for " + itemProduto.getDescricao(), Toast.LENGTH_SHORT).show();
                        item.delete();
                        list.clear();
                        popularLista();

                        break;
                }
                return false;
            }
        });




    }

    public void criaCarrinho(){
        carrinho = new Carrinho();
        carrinho.setId(0L);
        carrinho.setEnviada(0);
        carrinho.setEncerrada(0);
        carrinho.setIdCompra(Util.getUniqueId(this));
        Log.d("IDCOMPRA", "NULLO: " + carrinho.getIdCompra());
        carrinho.save();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_cadastro) {

            Intent intent = new Intent(this,IncluirNovo.class);
            startActivity(intent);

            return true;
        }
        if (id == R.id.action_editar) {

            Intent intent = new Intent(this,Editar.class);
            startActivity(intent);

            return true;
        }
        if (id == R.id.action_finalizar) {

            if(quantidadeItens>0){
                Intent intent = new Intent(this,FinalizarCompraActivity.class);
                intent.putExtra("quantidadeItens", quantidadeItens);
                intent.putExtra("valorTotal",valorTotal);
                intent.putExtra("idCompra",carrinho.getIdCompra());
                startActivityForResult(intent,555);
            }


            Intent intent = new Intent(this,FinalizarCompraActivity.class);
            startActivity(intent);

            return true;
        }

        if (id == R.id.action_sincronizar) {

//            infiniteBusyModifier = new InfiniteBusyModifier();
//            infiniteBusyModifier.execute();

            busyIndicator.setVisibility(View.VISIBLE);

            new APIClient().getRestService().getAllProdutos(callbackProdutos);


            return true;
        }

        if (id == R.id.action_mapa) {

//            infiniteBusyModifier = new InfiniteBusyModifier();
//            infiniteBusyModifier.execute();

            Intent intent = new Intent(this,Maps.class);
            startActivity(intent);

            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case IntentIntegrator.REQUEST_CODE:

                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode,
                        resultCode, data);
                if (scanResult == null) {
                    return;
                }
                String result = scanResult.getContents();
                if (result != null) {

                    Produto produto = Query.one(Produto.class, "select * from produto where codigo_barras = ?",result).get();
                    if(produto !=null) {
                        Item item = new Item();
                        item.setId(0L);
                        item.setIdCompra(carrinho.getIdCompra());
                        item.setQuantidade(1);
                        item.setIdProduto(produto.getCodigoBarras());
                        item.save();
                        popularLista();
                    }else{
                        Snackbar.make(coordinatorLayout, "Produto não cadastrado!", Snackbar.LENGTH_LONG);
                    }

                }
                break;
            case 555:
                    carrinho = Query.one(Carrinho.class, "select * from carrinho where id = (select max(id) from carrinho)").get();

                    if(carrinho == null){
                        criaCarrinho();
                    }else{
                        if(carrinho.getEncerrada()==1){
                            criaCarrinho();
                        }
                    }
                    list.clear();
                    popularLista();
                break;
            default:
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void popularLista(){
        CursorList cursor = Query.many(Item.class, "select * from Item where id_compra = ? order by id", carrinho.getIdCompra()).get();
        List<Item> listaItem =  cursor.asList();
        Log.d("TAMANHOLISTA",""+ listaItem.size());

        ItemProduto itemProduto;
        Produto produto;
        list = new ArrayList<>();
        valorTotal=0.0d;
        quantidadeItens=0;
        for(Item item:listaItem){

            produto = Query.one(Produto.class,"select * from Produto where codigo_barras = ?", item.getIdProduto()).get();
            itemProduto = new ItemProduto();
            itemProduto.setIdCompra(carrinho.getIdCompra());
            itemProduto.setIdItem(item.getId());
            itemProduto.setFoto(produto.getFoto());
            itemProduto.setDescricao(produto.getDescricao());
            itemProduto.setQuantidade(item.getQuantidade());
            itemProduto.setPreco(produto.getPreco());
            list.add(itemProduto);
            valorTotal+=item.getQuantidade()*produto.getPreco();
            quantidadeItens+=item.getQuantidade();

        }
        getSupportActionBar().setTitle("PDV"+Util.getCurrencyValue(valorTotal));
        adapter = new CustomArrayAdapter(this, R.layout.list_item, list);
        listView.setAdapter(adapter);
    }

    private void configureProdutoCallback() {

        callbackProdutos = new Callback<List<Produto>>() {

            @Override public void success(List<Produto> resultado, Response response) {

                CursorList cursorList = Query.all(Produto.class).get();

                List<Produto> lp = cursorList.asList();

                for(Produto p:lp){
                    p.delete();
                }

                for(Produto produto:resultado){
                    produto.setId(0L);
                    produto.save();
                }
                busyIndicator.setVisibility(View.INVISIBLE);

            }

            @Override public void failure(RetrofitError error) {

                Log.e("RETROFIT", "Error:"+error.getMessage());
            }
        };
    }

//    class BusyIndicatorAsyncTask extends AsyncTask {
//        @Override
//        protected Object doInBackground(Object[] params) {
//
//            for (int i = 0; i <= 102; i += 3) {
//                final int a = i;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        // NOTE : You can set the current value of BusyIndicator
//                        busyIndicator.setValue(a);
//                    }
//                });
//
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            return null;
//        }
//    }

    class InfiniteBusyModifier extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            final Random rand = new Random();

            FragmentActivity activity = MainActivity.this;

            while (true) {
                final int a = rand.nextInt(3) + 1;

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // NOTE : You can set the speed
                        busyIndicator.setVisibility(View.VISIBLE);
                        busyIndicator.setAngleModifier(a);
                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
