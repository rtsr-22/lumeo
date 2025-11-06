import { useState } from "react";
import Logout from "./Logout";
import MobileFooter from "./MobileFooter";

function Home() {
  const [showSearch, setShowSearch] = useState(false);

  return (
    <>
      <div className="h-screen bg-white">
      
        <div className="flex items-center justify-between px-6 py-4 shadow-md relative">
       
          <div className="flex items-center">
            <p className="text-4xl font-bold text-red-900 lg:text-6xl">lumoe</p>
            <img
              className="w-9 h-9 ml-2 lg:w-12 lg:h-12"
              src="./src/assets/play-svgrepo-com(1).svg"
              alt="logo"
            />
          </div>

          <div className="flex-1 mx-8 hidden md:flex justify-center">
            <div className="flex items-center w-full max-w-md border border-red-900 rounded-full overflow-hidden focus-within:ring-2 focus-within:ring-red-900 transition-all">
              <input
                className="flex-grow px-4 py-2 focus:outline-none"
                type="text"
                placeholder="Search..."
              />
              <button className=" px-4 py-2 flex items-center justify-center ">
                <img
                  className="w-5 h-5"
                  src="./src/assets/search-svgrepo-com.svg"
                  alt="search"
                />
              </button>
            </div>
          </div>

          <div className="md:hidden">
            <img
              className="w-8 h-8 cursor-pointer"
              src="./src/assets/search-svgrepo-com.svg"
              alt="search"
              onClick={() => setShowSearch(!showSearch)}
            />
          </div>

          <Logout />

          {showSearch && (
            <div className="absolute top-full left-0 w-full px-4 py-2 bg-white border-t border-gray-200 shadow-md md:hidden animate-slideDown">
              <div className="flex items-center border border-red-900 rounded-full overflow-hidden focus-within:ring-2 focus-within:ring-red-900 transition-all">
                <input
                  className="flex-grow px-4 py-2 focus:outline-none"
                  type="text"
                  placeholder="Search..."
                  autoFocus
                />
                <button className="bg-red-900 px-4 py-2 flex items-center justify-center hover:bg-red-800 transition">
                  <img
                    className="w-5 h-5 invert"
                    src="./src/assets/search-svgrepo-com.svg"
                    alt="search"
                  />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      <MobileFooter />
    </>
  );
}

export default Home;


