const outputElement = document.getElementById('output');

function updateOutput(message, isError = false) {
    const timestamp = new Date().toLocaleTimeString();
    const prefix = isError ? '❌ ERROR' : '✅ SUCCESS';
    outputElement.textContent = `[${timestamp}] ${prefix}: ${message}\n\n` + outputElement.textContent;
}

async function triggerError(errorType) {
    try {
        const response = await fetch(`/api/demo/errors/${errorType}`);
        const text = await response.text();
        
        if (!response.ok) {
            const errorInfo = `${response.status} - ${response.statusText}\n${text}`;
            updateOutput(errorInfo, true);
        } else {
            updateOutput(text);
        }
    } catch (error) {
        updateOutput(`Network error: ${error.message}`, true);
    }
}

async function triggerValidationError() {
    try {
        const response = await fetch('/api/demo/errors/validation-error', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ invalidField: 'test' }) // Missing required field
        });
        
        const text = await response.text();
        if (!response.ok) {
            updateOutput(`Validation Error: ${text}`, true);
        } else {
            updateOutput(text);
        }
    } catch (error) {
        updateOutput(`Network error: ${error.message}`, true);
    }
}

async function getProducts() {
    try {
        const response = await fetch('/api/products');
        const products = await response.json();
        updateOutput(`Found ${products.length} products:\n${JSON.stringify(products, null, 2)}`);
    } catch (error) {
        updateOutput(`Error fetching products: ${error.message}`, true);
    }
}

async function searchProducts() {
    const query = prompt('Enter search query:', 'iPhone');
    if (!query) return;
    
    try {
        const response = await fetch(`/api/products/search?query=${encodeURIComponent(query)}`);
        const products = await response.json();
        
        if (response.ok) {
            updateOutput(`Search results for "${query}":\n${JSON.stringify(products, null, 2)}`);
        } else {
            updateOutput(`Search failed: ${products.message || 'Unknown error'}`, true);
        }
    } catch (error) {
        updateOutput(`Search error: ${error.message}`, true);
    }
}

async function createProduct() {
    const productName = prompt('Enter product name:', 'Test Product ' + Date.now());
    if (!productName) return;
    
    const product = {
        name: productName,
        description: 'Test product created from demo UI',
        price: Math.floor(Math.random() * 1000) + 100,
        stock: Math.floor(Math.random() * 50) + 10,
        category: 'Test'
    };
    
    try {
        const response = await fetch('/api/products', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(product)
        });
        
        const result = await response.json();
        if (response.ok) {
            updateOutput(`Product created:\n${JSON.stringify(result, null, 2)}`);
        } else {
            updateOutput(`Failed to create product: ${JSON.stringify(result)}`, true);
        }
    } catch (error) {
        updateOutput(`Create product error: ${error.message}`, true);
    }
}

async function purchaseProduct() {
    const productId = prompt('Enter product ID to purchase:', '1');
    const quantity = prompt('Enter quantity:', '5');
    
    if (!productId || !quantity) return;
    
    try {
        const response = await fetch(`/api/products/${productId}/purchase?quantity=${quantity}`, {
            method: 'POST'
        });
        
        const result = await response.text();
        if (response.ok) {
            updateOutput(`Purchase successful:\n${result}`);
        } else {
            updateOutput(`Purchase failed: ${result}`, true);
        }
    } catch (error) {
        updateOutput(`Purchase error: ${error.message}`, true);
    }
}

async function bulkImport() {
    const products = [
        { name: 'Bulk Product 1', price: 99.99, stock: 10, category: 'Bulk' },
        { name: 'Bulk Product 2', price: 149.99, stock: 20, category: 'Bulk' },
        { name: 'iPhone 15', price: 999.99, stock: 5, category: 'Bulk' }, // Duplicate - will fail
        { name: 'Bulk Product 3', price: -50, stock: 15, category: 'Bulk' }, // Invalid price - will fail
        { name: 'Bulk Product 4', price: 199.99, stock: 30, category: 'Bulk' }
    ];
    
    try {
        const response = await fetch('/api/products/bulk-import', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(products)
        });
        
        const result = await response.json();
        updateOutput(`Bulk import results:\n${JSON.stringify(result, null, 2)}`, 
                    result.failureCount > 0);
    } catch (error) {
        updateOutput(`Bulk import error: ${error.message}`, true);
    }
}

// Auto-refresh task count every 10 seconds
setInterval(async () => {
    try {
        const response = await fetch('/');
        // Just to trigger a refresh - in a real app, we'd have an API endpoint
    } catch (error) {
        console.error('Auto-refresh error:', error);
    }
}, 10000);