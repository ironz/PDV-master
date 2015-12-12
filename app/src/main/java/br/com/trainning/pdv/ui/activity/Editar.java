package br.com.trainning.pdv.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

import com.mapzen.android.lost.api.LocationServices;
import com.mapzen.android.lost.api.LostApiClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.com.trainning.pdv.R;
import br.com.trainning.pdv.domain.image.Base64Util;
import br.com.trainning.pdv.domain.image.ImageInputHelper;
import br.com.trainning.pdv.domain.model.Produto;
import br.com.trainning.pdv.domain.network.APIClient;
import butterknife.Bind;
import jim.h.common.android.lib.zxing.config.ZXingLibConfig;
import jim.h.common.android.lib.zxing.integrator.IntentIntegrator;
import jim.h.common.android.lib.zxing.integrator.IntentResult;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import se.emilsjolander.sprinkles.CursorList;
import se.emilsjolander.sprinkles.Query;

public class Editar extends BaseActivity implements ImageInputHelper.ImageActionListener{


    @Bind(R.id.editText)
    EditText descricao;
    @Bind(R.id.editText2)
    EditText unidade;
    @Bind(R.id.editText3)
    EditText codigoBarras;
    @Bind(R.id.editText4)
    EditText preco;
    @Bind(R.id.imageView2)
    ImageView foto;
    @Bind(R.id.imageButton)
    ImageButton tiraFoto;
    @Bind(R.id.imageButton2)
    ImageButton selecionaGaleria;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.spinner1)
    Spinner spinner1;
    @Bind(R.id.btnCodigo)
    Button codigo;

    Produto produto;

    private ZXingLibConfig zxingLibConfig;


    private ImageInputHelper imageInputHelper;

    private Callback<String> callbackUpdateProduto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        configureProdutoCallback();

        LostApiClient lostApiClient = new LostApiClient.Builder(this).build();
        lostApiClient.connect();

        produto = new Produto();

        spinner1 = (Spinner) findViewById(R.id.spinner1);
        List<String> barcodeList = new ArrayList<>();
        List<Produto> produtoList;


        CursorList cursor = Query.many(Produto.class, "select * from produto where ativo = 0").get();
        produtoList = cursor.asList();

        for(Produto produto: produtoList){
            barcodeList.add(produto.getCodigoBarras());
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,barcodeList);

        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        spinner1.setAdapter(dataAdapter);


        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] barCode = new String[1];
                barCode[0] = parent.getItemAtPosition(position).toString();

                Log.d("BARCODE", barCode[0]);

                produto = Query.one(Produto.class, "select * from produto where codigo_barras = ?", barCode[0]).get();

                descricao.setText(produto.getDescricao());
                unidade.setText(produto.getUnidade());
                preco.setText(String.valueOf(produto.getPreco()));
                codigoBarras.setText(produto.getCodigoBarras());
                foto.setImageBitmap(Base64Util.decodeBase64(produto.getFoto()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                produto.setDescricao(descricao.getText().toString());
                produto.setUnidade(unidade.getText().toString());
                produto.setCodigoBarras(codigoBarras.getText().toString());
                produto.setPreco(Double.parseDouble(preco.getText().toString()));
                Bitmap imagem = ((BitmapDrawable) foto.getDrawable()).getBitmap();

                produto.setFoto(Base64Util.encodeTobase64(imagem));
                Location location = LocationServices.FusedLocationApi.getLastLocation();
                if (location != null) {
                    produto.setLatitude(location.getLatitude());
                    produto.setLongitude(location.getLongitude());
                }
                produto.save();
                new APIClient().getRestService().updateProduto(produto.getCodigoBarras(), produto.getDescricao(), produto.getUnidade(), produto.getPreco(), produto.getFoto(), produto.getAtivo(), produto.getLatitude(), produto.getLongitude(), callbackUpdateProduto);
                finish();
            }
        });

        tiraFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageInputHelper.takePhotoWithCamera();
            }
        });

        selecionaGaleria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageInputHelper.selectImageFromGallery();
            }
        });

        codigo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                IntentIntegrator.initiateScan(Editar.this, zxingLibConfig);

            }
        });

        imageInputHelper = new ImageInputHelper(this);
        imageInputHelper.setImageActionListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageInputHelper.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        imageInputHelper.onActivityResult(requestCode, resultCode, data);
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
                    codigoBarras.setText(String.valueOf(result));

//                }else{
//                        Snackbar.make(coordinatorLayout, "Produto não cadastrado!", Snackbar.LENGTH_LONG);
//                    }

                }
                break;
        }


    }

    @Override
    public void onImageSelectedFromGallery(Uri uri, File imageFile) {
        // cropping the selected image. crop intent will have aspect ratio 16/9 and result image
        // will have size 800x450
        imageInputHelper.requestCropImage(uri, 100, 100, 1, 1);
    }

    @Override
    public void onImageTakenFromCamera(Uri uri, File imageFile) {
        // cropping the taken photo. crop intent will have aspect ratio 16/9 and result image
        // will have size 800x450
        imageInputHelper.requestCropImage(uri, 100, 100, 1, 1);
    }

    @Override
    public void onImageCropped(Uri uri, File imageFile) {
        try {
            // getting bitmap from uri
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            // showing bitmap in image view
            foto.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureProdutoCallback() {

        callbackUpdateProduto = new Callback<String>() {

            @Override public void success(String resultado, Response response) {
                Log.d("RETROFIT", "ENVIADO COM SUCESSO");
            }

            @Override public void failure(RetrofitError error) {

                Log.e("RETROFIT", "Error:"+error.getMessage());
            }
        };
    }
}
