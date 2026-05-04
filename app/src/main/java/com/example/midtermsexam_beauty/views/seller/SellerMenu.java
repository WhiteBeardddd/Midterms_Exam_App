package com.example.midtermsexam_beauty.views.seller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.MenuItemAdapter;
import com.example.midtermsexam_beauty.adapters.SellerNavCard;
import com.example.midtermsexam_beauty.models.MenuItem;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.bumptech.glide.Glide;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SellerMenu extends AppCompatActivity {

    private static final String TAG = "SellerMenu";

    private RecyclerView rvMenu;
    private ProgressBar loader;
    private LinearLayout emptyState;
    private MenuItemAdapter adapter;
    private Button btnAddItem;

    private final SupabaseAuthService supabase = new SupabaseAuthService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String token, sellerId, authId;
    private Uri selectedImageUri = null;
    private ImageView dialogImagePreview = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (dialogImagePreview != null && selectedImageUri != null) {
                        Glide.with(this).load(selectedImageUri).into(dialogImagePreview);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_menu);
        SellerNavCard.setupNavbar(this);

        rvMenu = findViewById(R.id.rvMenu);
        loader = findViewById(R.id.loader);
        emptyState = findViewById(R.id.emptyState);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnAddItem.setEnabled(false);

        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        btnAddItem.setOnClickListener(v -> showMenuItemDialog(null));

        SessionManager session = new SessionManager(this);
        token = session.getToken();
        sellerId = session.getSellerId();
        authId = session.getUserId();

        if (token != null) {
            try {
                String[] parts = token.split("\\.");
                String payload = new String(android.util.Base64.decode(parts[1],
                        android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING));
                org.json.JSONObject jwt = new org.json.JSONObject(payload);
                long exp = jwt.getLong("exp");
                long now = System.currentTimeMillis() / 1000;
                Log.d(TAG, "Token exp: " + exp + " now: " + now + " expired? " + (now > exp));
            } catch (Exception e) {
                Log.e(TAG, "Token decode error", e);
            }
        }
        Log.d(TAG, "token null? " + (token == null));
        Log.d(TAG, "sellerId from session: " + sellerId);
        Log.d(TAG, "authId: " + authId);

        if (sellerId != null) {
            btnAddItem.setEnabled(true);
            loadMenuItems();
        } else {
            loader.setVisibility(View.VISIBLE);
            executor.execute(() -> {
                String resolved = supabase.getSellerIdByAuthId(token, authId);
                Log.d(TAG, "Resolved sellerId: " + resolved);
                handler.post(() -> {
                    loader.setVisibility(View.GONE);
                    if (resolved == null) {
                        Toast.makeText(this, "Seller profile not found.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    sellerId = resolved;
                    session.setSellerId(resolved);
                    btnAddItem.setEnabled(true);
                    loadMenuItems();
                });
            });
        }
    }

    private void loadMenuItems() {
        if (sellerId == null) {
            Log.e(TAG, "loadMenuItems called with null sellerId");
            return;
        }
        loader.setVisibility(View.VISIBLE);
        rvMenu.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        executor.execute(() -> {
            List<MenuItem> items = supabase.getMenuItems(token, sellerId);
            Log.d(TAG, "Loaded " + items.size() + " items");
            handler.post(() -> {
                loader.setVisibility(View.GONE);
                if (items.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvMenu.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvMenu.setVisibility(View.VISIBLE);
                    adapter = new MenuItemAdapter(
                            items,
                            this::showMenuItemDialog,
                            this::confirmDelete
                    );
                    rvMenu.setAdapter(adapter);
                }
            });
        });
    }

    private void toggleAvailability(MenuItem item) {
        executor.execute(() -> {
            Log.d(TAG, "Toggling " + item.getId() + " to " + item.isAvailable());
            boolean success = supabase.updateMenuItemAvailability(token, item.getId(), item.isAvailable());
            handler.post(() -> {
                if (!success) {
                    Toast.makeText(this, "Failed to update availability.", Toast.LENGTH_SHORT).show();
                    item.setAvailable(!item.isAvailable());
                    if (adapter != null) adapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void showMenuItemDialog(MenuItem existing) {
        boolean isEdit = existing != null;
        selectedImageUri = null;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_menu_item, null);
        EditText etName        = view.findViewById(R.id.etName);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etPrice       = view.findViewById(R.id.etPrice);
        EditText etCategory    = view.findViewById(R.id.etCategory);
        ImageView ivPreview    = view.findViewById(R.id.ivImagePreview);
        Button btnPickImage    = view.findViewById(R.id.btnPickImage);
        androidx.appcompat.widget.SwitchCompat switchAvailable = view.findViewById(R.id.switchAvailable);

        dialogImagePreview = ivPreview;

        if (isEdit) {
            etName.setText(existing.getName());
            etDescription.setText(existing.getDescription());
            etPrice.setText(String.valueOf(existing.getPrice()));
            etCategory.setText(existing.getCategory());
            switchAvailable.setChecked(existing.isAvailable()); // set current value
            if (existing.getImageUrl() != null && !existing.getImageUrl().isEmpty()) {
                Glide.with(this).load(existing.getImageUrl()).into(ivPreview);
            }
        } else {
            switchAvailable.setChecked(true); // default to available for new items
        }

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Edit Item" : "Add Item")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name     = etName.getText().toString().trim();
                    String desc     = etDescription.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();
                    String cat      = etCategory.getText().toString().trim();
                    boolean isAvailable = switchAvailable.isChecked(); // get toggle value

                    if (name.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(this, "Name and price are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price;
                    try { price = Double.parseDouble(priceStr); }
                    catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid price.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    MenuItem item = isEdit ? existing : new MenuItem();
                    item.setSellerId(sellerId);
                    item.setName(name);
                    item.setDescription(desc);
                    item.setPrice(price);
                    item.setCategory(cat);
                    item.setAvailable(isAvailable); // use toggle value

                    saveMenuItem(item, isEdit);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveMenuItem(MenuItem item, boolean isEdit) {
        loader.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            Log.d(TAG, "=== saveMenuItem ===");
            Log.d(TAG, "isEdit: " + isEdit);
            Log.d(TAG, "item.getId(): " + item.getId());
            Log.d(TAG, "item.getSellerId(): " + item.getSellerId());
            Log.d(TAG, "item.getName(): " + item.getName());
            Log.d(TAG, "selectedImageUri: " + selectedImageUri);

            if (selectedImageUri != null) {
                if (isEdit && item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                    String oldPath = extractPathFromUrl(item.getImageUrl());
                    Log.d(TAG, "Deleting old image path: " + oldPath);
                    if (oldPath != null) supabase.deleteImage(token, "menu-items", oldPath);
                }
                String imageUrl = uploadImage(selectedImageUri);
                Log.d(TAG, "Uploaded new image url: " + imageUrl);
                if (imageUrl != null) item.setImageUrl(imageUrl);
            }

            boolean success;
            if (isEdit) {
                Log.d(TAG, "Calling updateMenuItem with id: " + item.getId());
                success = supabase.updateMenuItem(token, item);
            } else {
                String newId = supabase.addMenuItem(token, item);
                Log.d(TAG, "addMenuItem returned id: " + newId);
                success = newId != null;
                if (success) item.setId(newId);
            }

            Log.d(TAG, "saveMenuItem success: " + success);
            handler.post(() -> {
                loader.setVisibility(View.GONE);
                Toast.makeText(this, success ? (isEdit ? "Item updated!" : "Item added!") : "Failed to save item.", Toast.LENGTH_SHORT).show();
                if (success) loadMenuItems();
            });
        });
    }

    // Extracts "authId/filename.jpg" from the full public URL
    private String extractPathFromUrl(String imageUrl) {
        try {
            String marker = "/object/public/menu-items/";
            int idx = imageUrl.indexOf(marker);
            if (idx != -1) return imageUrl.substring(idx + marker.length());
        } catch (Exception e) { Log.e(TAG, "extractPathFromUrl error", e); }
        return null;
    }


    private String uploadImage(Uri uri) {
        try {
            Log.d(TAG, "uploadImage uri: " + uri + " authId: " + authId);
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                Log.e(TAG, "InputStream is null");
                return null;
            }
            byte[] data = readStreamBytes(is);
            is.close();

            Log.d(TAG, "image bytes: " + data.length);

            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";
            String ext  = mimeType.contains("png") ? "png" : "jpg";
            String path = authId + "/" + UUID.randomUUID() + "." + ext;

            return supabase.uploadImage(token, "menu-items", path, data, mimeType);
        } catch (Exception e) {
            Log.e(TAG, "uploadImage error", e);
            return null;
        }
    }

    private byte[] readStreamBytes(InputStream is) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] chunk = new byte[4096];
        while ((nRead = is.read(chunk, 0, chunk.length)) != -1) {
            buffer.write(chunk, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private void confirmDelete(MenuItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Remove \"" + item.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    loader.setVisibility(View.VISIBLE);
                    executor.execute(() -> {
                        // 1. Delete image from storage FIRST
                        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                            String oldPath = extractPathFromUrl(item.getImageUrl());
                            if (oldPath != null) {
                                supabase.deleteImage(token, "menu-items", oldPath);
                                Log.d(TAG, "Deleted image: " + oldPath);
                            }
                        }
                        // 2. Then delete the DB record
                        boolean success = supabase.deleteMenuItem(token, item.getId());
                        handler.post(() -> {
                            loader.setVisibility(View.GONE);
                            Toast.makeText(this, success ? "Deleted." : "Failed to delete.", Toast.LENGTH_SHORT).show();
                            if (success) loadMenuItems();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}