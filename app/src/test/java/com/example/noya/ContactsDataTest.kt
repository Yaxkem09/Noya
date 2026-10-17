package com.example.noya

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.noya.data.SaveContactResult
import com.example.noya.data.getContactInfoFromNumber
import com.example.noya.data.getContacts
import com.example.noya.data.saveContactWithPhoto
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Lista de contactos, búsqueda por número y "Nuevo contacto".
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContactsDataTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var contacts: FakeContactsProvider

    @Before
    fun setUp() {
        contacts = FakeContactsProvider.install()
    }

    @Test
    fun listaDeContactosOrdenadaYSinDuplicados() {
        contacts.rows.addAll(
            listOf(
                FakeContactsProvider.Row("1", "Pedro", "5552222222"),
                FakeContactsProvider.Row("2", "Ana", "555 111 1111"),
                FakeContactsProvider.Row("3", "Ana (otro)", "555-111-1111"), // mismo número
                FakeContactsProvider.Row("4", null, "5553333333"),
                FakeContactsProvider.Row("5", "Sin número", "")
            )
        )

        val result = getContacts(context, forceRefresh = true)

        // Los contactos sin nombre van primero (así ordena la base de datos de Android)
        assertEquals(listOf("Sin nombre", "Ana", "Pedro"), result.map { it.name })
        // De los duplicados se queda el primero
        assertEquals("555 111 1111", result[1].phoneNumber)
    }

    @Test
    fun buscaNombreYFotoPorNumero() {
        contacts.rows.add(FakeContactsProvider.Row("1", "Juan", "5551111111", "content://foto/juan"))

        val (name, photo) = getContactInfoFromNumber(context, "5551111111")

        assertEquals("Juan", name)
        assertEquals("content://foto/juan", photo)
    }

    @Test
    fun numeroQueNoEstaEnContactos() {
        val (name, photo) = getContactInfoFromNumber(context, "5550000000")

        assertNull(name)
        assertNull(photo)
    }

    @Test
    fun guardarContactoSinFoto() {
        val result = saveContactWithPhoto(context, "Lucía", "5554444444", null)

        assertEquals(SaveContactResult.SAVED, result)
        assertEquals(1, contacts.insertedRawContacts.size)

        val name = contacts.insertedData.single {
            it.getAsString(ContactsContract.Data.MIMETYPE) == ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
        }
        assertEquals("Lucía", name.getAsString(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME))
        assertEquals(1L, name.getAsLong(ContactsContract.Data.RAW_CONTACT_ID))

        val phone = contacts.insertedData.single {
            it.getAsString(ContactsContract.Data.MIMETYPE) == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
        }
        assertEquals("5554444444", phone.getAsString(ContactsContract.CommonDataKinds.Phone.NUMBER))
    }

    @Test
    fun guardarContactoConFotoGrandeLaReduce() {
        val photoUri = createPhoto(width = 4000, height = 3000)

        val result = saveContactWithPhoto(context, "Lucía", "5554444444", photoUri)

        assertEquals(SaveContactResult.SAVED, result)
        val photoRow = contacts.insertedData.single {
            it.getAsString(ContactsContract.Data.MIMETYPE) == ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
        }
        val bytes = photoRow.getAsByteArray(ContactsContract.CommonDataKinds.Photo.PHOTO)
        assertNotNull(bytes)

        val saved = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertTrue("La foto debe quedar en máximo 720px", maxOf(saved.width, saved.height) <= 720)
        // Mantiene la proporción 4:3
        assertEquals(720, saved.width)
        assertEquals(540, saved.height)
    }

    @Test
    fun fotoIlegibleGuardaElContactoSinFoto() {
        val file = File(context.cacheDir, "no_es_foto.jpg").apply { writeText("esto no es una imagen") }

        val result = saveContactWithPhoto(context, "Lucía", "5554444444", Uri.fromFile(file))

        assertEquals(SaveContactResult.SAVED_WITHOUT_PHOTO, result)
        assertEquals(1, contacts.insertedRawContacts.size)
    }

    private fun createPhoto(width: Int, height: Int): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val file = File(context.cacheDir, "foto_grande.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }
}
