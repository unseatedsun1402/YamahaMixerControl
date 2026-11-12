package MidiControl.HardwareClients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import MidiControl.Controls.CanonicalRegistry;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class HardwareClientManagerTest {
    
    @Mock
    private CanonicalRegistry mockRegistry;
    
    @Mock
    private IControlClient mockClient;
    
    private HardwareClientManager clientManager;
    
    @BeforeEach
    void setUp() {
        when(mockClient.getClientId()).thenReturn("test-client");
        when(mockClient.getDisplayName()).thenReturn("Test Client");
        when(mockClient.getClientType()).thenReturn(IControlClient.ClientType.CUSTOM);
        when(mockClient.isRunning()).thenReturn(true);
        
        clientManager = new HardwareClientManager(mockRegistry);
    }
    
    @Test
    void testRegisterClient() {
        // When
        clientManager.registerClient(mockClient);
        
        // Then
        assertEquals(1, clientManager.getAllClients().size());
        assertEquals(mockClient, clientManager.getClient("test-client"));
        
        verify(mockClient).onRegistryReload(mockRegistry);
    }
    
    @Test
    void testUnregisterClient() {
        // Given
        clientManager.registerClient(mockClient);
        
        // When
        clientManager.unregisterClient("test-client");
        
        // Then
        assertEquals(0, clientManager.getAllClients().size());
        assertNull(clientManager.getClient("test-client"));
        
        verify(mockClient).stop();
    }
    
    @Test
    void testStartAll() {
        // Given
        clientManager.registerClient(mockClient);
        
        // When
        clientManager.startAll();
        
        // Then
        verify(mockClient).start();
    }
    
    @Test
    void testStopAll() {
        // Given
        clientManager.registerClient(mockClient);
        
        // When
        clientManager.stopAll();
        
        // Then
        verify(mockClient).stop();
    }
    
    @Test
    void testHandleHardwareInput() {
        // This test would require more complex setup with a real registry
        // For now, just verify the method doesn't throw
        assertDoesNotThrow(() -> {
            clientManager.handleHardwareInput("test-client", "channel.1.level", 64);
        });
    }
    
    @Test
    void testOnRegistryReload() {
        // Given
        CanonicalRegistry newRegistry = mock(CanonicalRegistry.class);
        clientManager.registerClient(mockClient);
        
        // When
        clientManager.onRegistryReload(newRegistry);
        
        // Then
        verify(mockClient).onRegistryReload(newRegistry);
    }
}