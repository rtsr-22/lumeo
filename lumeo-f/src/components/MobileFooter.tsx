// export default function MobileFooter() {
//   return (
//     <>
//       <div className="md:hidden">
//         <div className="flex justify-between">
//           <img
//             src="./src/assets/home_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.svg"
//             alt="Home"
//           />
//           <img src="./src/assets/movie-svgrepo-com.svg" alt="Create" />
//           <img
//             className="w-8 h-8 cursor-pointer"
//             src="./src/assets/user-svgrepo-com.svg"
//             alt="Profile"
//           />
//         </div>
//       </div>
//     </>
//   );
// }

import { useState, useRef, useEffect } from "react";

export default function MobileFooter() {
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const toggleMenu = () => setOpen((prev) => !prev);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <footer className="md:hidden fixed bottom-0 left-0 right-0 bg-white shadow-lg p-3 z-50">
      <div className="flex justify-around items-center">
        
        {/* Home */}
        <img
          src="./src/assets/home_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.svg"
          alt="Home"
          className="w-7 h-7 cursor-pointer"
        />

        {/* Movie */}
        <img
          src="./src/assets/movie-svgrepo-com.svg"
          alt="Movie"
          className="w-7 h-7 cursor-pointer"
        />

        {/* Profile */}
        <div ref={menuRef} className="relative">
          <img
            className="w-6 h-6 cursor-pointer"
            src="./src/assets/user-svgrepo-com.svg"
            alt="Profile"
            onClick={toggleMenu}
          />

          {/* Dropdown */}
          {open && (
            <div className="absolute bottom-12 right-0 w-32 bg-white shadow-lg border rounded-md py-1">
              <button className="block w-full text-left px-3 py-2 hover:bg-gray-200">
                Profile
              </button>
              <button className="block w-full text-left px-3 py-2 hover:bg-gray-200 text-red-600">
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </footer>
  );
}
