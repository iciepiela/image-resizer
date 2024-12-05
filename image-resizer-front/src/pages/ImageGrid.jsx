import React, { useState, useEffect } from 'react';
import { fromEvent } from 'rxjs';
import { map } from 'rxjs/operators';

const ImageGrid = () => {
    const [images, setImages] = useState([]);

    const loadPhotos = () => {
        if (images.length > 3) return;
        const eventSource = new EventSource('http://localhost:8080/images/original');

        const imageStream$ = fromEvent(eventSource, 'message').pipe(
            map((event) => {
                const parsedData = JSON.parse(event.data)
                return parsedData.body;
            })
        );

        const subscription = imageStream$.subscribe({
            next: (imageData) => {
                setImages((prevImages) => [...prevImages, imageData]);
            },
            error: (err) => {
                console.error('Error receiving image data:', err);
            },
            complete: () => { console.log("Complete") }
        });


        return () => {
            eventSource.close();
            subscription.unsubscribe();
        };
    };

    useEffect(() => console.log(images.length), [images])

    return (
        <div>
            <button onClick={loadPhotos}>Load</button>
            {images.map((image, index) => (
                <div key={index} style={{ margin: '10px', display: 'inline-block' }}>
                    <img
                        src={`data:image/jpeg;base64,${image}`}
                        alt={`Image ${index}`}
                        style={{ width: '200px', height: 'auto', borderRadius: '8px' }}
                    />
                </div>
            ))}
        </div>
    );
};
export default ImageGrid;
