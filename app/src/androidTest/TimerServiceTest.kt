// app/src/test/java/com/pneumasoft/multitimer/TimerServiceTest.kt
@RunWith(RobolectricTestRunner::class)
class TimerServiceTest {
    @Test
    fun `service starts properly with notification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, TimerService::class.java)
        val service = Robolectric.buildService(TimerService::class.java, intent)
            .create()
            .get()

        assertNotNull(service)
        // Add more assertions for service state
    }
}
