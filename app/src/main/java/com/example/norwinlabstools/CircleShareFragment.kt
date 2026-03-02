package com.example.norwinlabstools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.norwinlabstools.databinding.FragmentLocationSharingBinding
import com.example.norwinlabstools.databinding.ItemCircleMemberBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*
import com.yalantis.ucrop.UCrop
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

class CircleShareFragment : Fragment() {

    private var _binding: FragmentLocationSharingBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationManager: LocationManager
    private val markers = mutableMapOf<String, Marker>()
    
    private var database: DatabaseReference? = null
    private var currentCircleId: String? = null
    private var userId: String = ""
    private var isAdmin: Boolean = false
    private var myName: String = "User"
    private var myPhotoBase64: String? = null

    private val PREFS_NAME = "circle_prefs"
    private val KEY_CIRCLE_ID = "current_circle_id"
    private val KEY_USER_ID = "user_id"
    private val KEY_MY_NAME = "user_name"
    private val KEY_MY_PHOTO = "user_photo"

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val sourceUri: Uri? = result.data?.data
            sourceUri?.let { uri ->
                startCrop(uri)
            }
        }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(uri))
                updateMyPhoto(bitmap)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(context, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val myPoint = GeoPoint(location.latitude, location.longitude)
            updateMarker("me", myPoint, isMe = true, name = myName, photoBase64 = myPhotoBase64)

            currentCircleId?.let { circleId ->
                database?.child("circles")?.child(circleId)?.child("members")?.child(userId)?.apply {
                    child("lat").setValue(location.latitude)
                    child("lng").setValue(location.longitude)
                    child("name").setValue(myName)
                    child("photo").setValue(myPhotoBase64)
                    child("lastUpdated").setValue(System.currentTimeMillis())
                }
            }
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        Configuration.getInstance().userAgentValue = requireContext().packageName

        _binding = FragmentLocationSharingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMap()
        loadUserData()

        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        binding.ivMyProfilePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            photoPickerLauncher.launch(intent)
        }

        binding.editMyName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                myName = s.toString()
                requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_MY_NAME, myName).apply()
                syncProfileToFirebase()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnCreateCircle.setOnClickListener {
            val newCode = (100000..999999).random().toString()
            createCircle(newCode)
        }

        binding.btnJoinCircle.setOnClickListener {
            val code = binding.editCircleCode.text.toString()
            if (code.length == 6) {
                joinCircle(code)
            } else {
                Toast.makeText(context, "Enter a 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnViewMembers.setOnClickListener {
            showMembersSheet()
        }

        binding.fabCenterOnMe.setOnClickListener {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                binding.mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
            } ?: Toast.makeText(context, "Finding location...", Toast.LENGTH_SHORT).show()
        }

        checkLocationPermissions()
        startLocationUpdates()
        
        currentCircleId?.let { joinCircle(it, isAutoJoin = true) }
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "profile_crop.jpg"))
        val uCrop = UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(200, 200)
        
        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    private fun loadUserData() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        userId = prefs.getString(KEY_USER_ID, null) ?: UUID.randomUUID().toString().take(6).also {
            prefs.edit().putString(KEY_USER_ID, it).apply()
        }
        currentCircleId = prefs.getString(KEY_CIRCLE_ID, null)
        myName = prefs.getString(KEY_MY_NAME, "User $userId") ?: "User $userId"
        myPhotoBase64 = prefs.getString(KEY_MY_PHOTO, null)

        binding.editMyName.setText(myName)
        myPhotoBase64?.let {
            val decodedString = Base64.decode(it, Base64.DEFAULT)
            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            binding.ivMyProfilePhoto.setImageBitmap(getCircularBitmap(decodedByte))
        }
    }

    private fun updateMyPhoto(bitmap: Bitmap) {
        val circular = getCircularBitmap(bitmap)
        val outputStream = ByteArrayOutputStream()
        circular.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        myPhotoBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
        
        binding.ivMyProfilePhoto.setImageBitmap(circular)
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_MY_PHOTO, myPhotoBase64).apply()
        syncProfileToFirebase()
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, bitmap.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun createMarkerIcon(photo: Bitmap?, isMe: Boolean): Bitmap {
        val size = 100
        val pointerHeight = 20
        val border = 6
        
        val output = Bitmap.createBitmap(size, size + pointerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = Path()
        
        val markerColor = if (isMe) 0xFF2196F3.toInt() else 0xFF4CAF50.toInt()
        
        paint.color = markerColor
        path.moveTo(size / 2f - 15, size - 5f)
        path.lineTo(size / 2f, size + pointerHeight.toFloat())
        path.lineTo(size / 2f + 15, size - 5f)
        path.close()
        canvas.drawPath(path, paint)
        
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - border, paint)
        
        if (photo != null) {
            val scaledPhoto = Bitmap.createScaledBitmap(photo, size - border * 2, size - border * 2, true)
            val circularPhoto = getCircularBitmap(scaledPhoto)
            canvas.drawBitmap(circularPhoto, border.toFloat(), border.toFloat(), null)
        } else {
            paint.color = markerColor
            paint.textSize = 40f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("?", size / 2f, size / 2f + 15, paint)
        }
        
        return output
    }

    private fun syncProfileToFirebase() {
        currentCircleId?.let { circleId ->
            database?.child("circles")?.child(circleId)?.child("members")?.child(userId)?.apply {
                child("name").setValue(myName)
                child("photo").setValue(myPhotoBase64)
            }
        }
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        val mapController = binding.mapView.controller
        mapController.setZoom(15.0)
    }

    private fun createCircle(circleId: String) {
        initDatabase()
        isAdmin = true
        database?.child("circles")?.child(circleId)?.child("admin")?.setValue(userId)
        joinCircle(circleId)
    }

    private fun joinCircle(circleId: String, isAutoJoin: Boolean = false) {
        initDatabase()
        
        currentCircleId?.let { oldId ->
            database?.child("circles")?.child(oldId)?.child("members")?.removeEventListener(circleValueListener)
            database?.child("circles")?.child(oldId)?.child("admin")?.removeEventListener(adminValueListener)
        }

        currentCircleId = circleId
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_CIRCLE_ID, circleId).apply()
        
        binding.tvCircleStatus.text = "Circle: $circleId"
        binding.btnViewMembers.visibility = View.VISIBLE
        binding.btnShareCode.visibility = View.VISIBLE
        binding.editCircleCode.setText(circleId)

        database?.child("circles")?.child(circleId)?.child("members")?.addValueEventListener(circleValueListener)
        database?.child("circles")?.child(circleId)?.child("admin")?.addValueEventListener(adminValueListener)
        
        syncProfileToFirebase()
        
        if (!isAutoJoin) {
            Toast.makeText(context, "Joined circle $circleId", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initDatabase() {
        if (database == null) {
            try {
                database = FirebaseDatabase.getInstance().reference
            } catch (e: Exception) {}
        }
    }

    private val adminValueListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val adminId = snapshot.getValue(String::class.java)
            isAdmin = (adminId == userId)
        }
        override fun onCancelled(error: DatabaseError) {}
    }

    private val circleValueListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.hasChild(userId) && !isAdmin && currentCircleId != null) {
                leaveCircle()
                return
            }

            val currentMemberIds = mutableSetOf<String>()
            snapshot.children.forEach { child ->
                val id = child.key ?: return@forEach
                currentMemberIds.add(id)
                if (id == userId) return@forEach
                
                val lat = child.child("lat").getValue(Double::class.java) ?: 0.0
                val lng = child.child("lng").getValue(Double::class.java) ?: 0.0
                val name = child.child("name").getValue(String::class.java) ?: "User $id"
                val photo = child.child("photo").getValue(String::class.java)
                updateMarker(id, GeoPoint(lat, lng), isMe = false, name = name, photoBase64 = photo)
            }
            
            val iterator = markers.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key != "me" && !currentMemberIds.contains(entry.key)) {
                    binding.mapView.overlays.remove(entry.value)
                    iterator.remove()
                }
            }
            binding.mapView.invalidate()
        }
        override fun onCancelled(error: DatabaseError) {}
    }

    private fun leaveCircle() {
        currentCircleId = null
        isAdmin = false
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_CIRCLE_ID).apply()
        
        binding.tvCircleStatus.text = "Join or Create a Circle"
        binding.btnViewMembers.visibility = View.GONE
        binding.btnShareCode.visibility = View.GONE
        binding.editCircleCode.setText("")
        
        val iterator = markers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key != "me") {
                binding.mapView.overlays.remove(entry.value)
                iterator.remove()
            }
        }
        binding.mapView.invalidate()
    }

    private fun updateMarker(id: String, position: GeoPoint, isMe: Boolean, name: String, photoBase64: String?) {
        val photoBitmap = photoBase64?.let {
            val decodedString = Base64.decode(it, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        }
        
        val markerIcon = createMarkerIcon(photoBitmap, isMe)
        
        if (markers.containsKey(id)) {
            val marker = markers[id]
            marker?.position = position
            marker?.title = name
            marker?.icon = BitmapDrawable(resources, markerIcon)
        } else {
            val marker = Marker(binding.mapView)
            marker.position = position
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = name
            marker.icon = BitmapDrawable(resources, markerIcon)
            
            binding.mapView.overlays.add(marker)
            markers[id] = marker
        }
        binding.mapView.invalidate()
    }

    private fun showMembersSheet() {
        if (currentCircleId == null) return
        val dialog = BottomSheetDialog(requireContext())
        
        val root = layoutInflater.inflate(R.layout.layout_add_tools, null)
        val title = root.findViewById<TextView>(R.id.textview_add_tools_title)
        title.text = "Circle Members"
        
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerview_available_tools)
        recycler.layoutManager = LinearLayoutManager(context)
        
        database?.child("circles")?.child(currentCircleId!!)?.child("members")?.get()?.addOnSuccessListener { snapshot ->
            val members = mutableListOf<CircleMember>()
            snapshot.children.forEach { child ->
                val member = CircleMember(
                    id = child.key ?: "",
                    name = child.child("name").getValue(String::class.java) ?: "User",
                    photoBase64 = child.child("photo").getValue(String::class.java)
                )
                members.add(member)
            }
            recycler.adapter = MemberAdapter(members, isAdmin, userId) { memberId ->
                removeMember(memberId)
                dialog.dismiss()
            }
            dialog.setContentView(root)
            dialog.show()
        }
    }

    private fun removeMember(memberId: String) {
        if (!isAdmin) return
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Member")
            .setMessage("Remove user from circle?")
            .setPositiveButton("Remove") { _, _ ->
                database?.child("circles")?.child(currentCircleId!!)?.child("members")?.child(memberId)?.removeValue()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                locationManager.requestLocationUpdates(provider, 5000L, 5f, locationListener)
            }
        } catch (e: Exception) {}
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    data class CircleMember(val id: String, val name: String, val photoBase64: String?)

    class MemberAdapter(
        private val members: List<CircleMember>,
        private val isAdmin: Boolean,
        private val currentUserId: String,
        private val onRemove: (String) -> Unit
    ) : RecyclerView.Adapter<MemberAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemCircleMemberBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCircleMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val member = members[position]
            holder.binding.tvMemberId.text = if (member.id == currentUserId) "${member.name} (You)" else member.name
            
            member.photoBase64?.let {
                val decodedString = Base64.decode(it, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                holder.binding.ivMemberAvatar.setImageBitmap(decodedByte)
            }

            if (isAdmin && member.id != currentUserId) {
                holder.binding.btnRemoveMember.visibility = View.VISIBLE
                holder.binding.btnRemoveMember.setOnClickListener { onRemove(member.id) }
            } else {
                holder.binding.btnRemoveMember.visibility = View.GONE
            }
        }

        override fun getItemCount() = members.size
    }

    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.removeUpdates(locationListener)
        _binding = null
    }
}
